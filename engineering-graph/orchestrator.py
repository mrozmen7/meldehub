"""Orchestrator — Engineering Graph'in kalbi.

Graf yapısı:

    START → planner → implementer → quality_gate ──kırmızı──┐
                        ▲                    │              │
                        │                    ▼ yeşil        │ (retry,
                        │              security_gate        │  max 3)
                        │                    │              │
                        │                    ▼              │
                        └───────3. deneme── reviewer        │
                                             │              │
                                             ▼              │
                                    ┌── human_approval ─────┘
                                    │  (interrupt: sistem
                                    │   burada DURUR ve
                                    │   insanı bekler)
                                    ▼
                            merge / reject → END

Kullanım (Faz 3 — iki mod):

  sim   : ./.venv/bin/python orchestrator.py "DLQ retry ekle" [merge|reject]
          Tek süreçte uçtan uca simülasyon (Faz 2 davranışı).

  live  : ./.venv/bin/python orchestrator.py live "<hedef>"
          Graf delegasyon interrupt'ında DURUR ve süreç çıkar.
          Uzman agent kodu yazar, kanıtı checkpoints/evidence.md'ye bırakır.
          ./.venv/bin/python orchestrator.py resume <thread_id> implemented
          → gerçek gate'ler çalışır, graf insan onayında tekrar durur.
          ./.venv/bin/python orchestrator.py resume <thread_id> merge
          → akış tamamlanır.

  live modunda checkpointer SqliteSaver'dır: durum diske yazılır,
  böylece graf süreçler arasında (ayrı komut çalıştırmalarında)
  kaldığı yerden devam edebilir. sim modunda MemorySaver yeterlidir
  çünkü her şey tek süreçte olur.
"""

import re
import sqlite3
import sys
from pathlib import Path

from langgraph.checkpoint.memory import MemorySaver
from langgraph.checkpoint.sqlite import SqliteSaver
from langgraph.graph import END, START, StateGraph
from langgraph.types import Command, interrupt

from gates.quality_gate import quality_gate_node
from gates.security_gate import security_gate_node
from nodes.implementer import implementer_node
from nodes.planner import planner_node
from nodes.reviewer import reviewer_node
from state import GraphState

DB_PATH = Path(__file__).resolve().parent / "checkpoints" / "graph.db"


# --- İnsan onay düğümü: sistemin DURDUĞU ikinci nokta -------------------------
def human_approval_node(state: GraphState) -> dict:
    """interrupt() çağrısı grafiği burada dondurur.

    İnsan karar verene kadar hiçbir düğüm çalışmaz. Karar,
    Command(resume=...) ile bu noktaya geri beslenir.
    """
    decision = interrupt({
        "type": "approve",
        "message": "Merge onayı bekleniyor",
        "spec": state["spec"],
        "quality_report": state["quality_report"],
        "security_report": state["security_report"],
        "review_report": state["review_report"],
        "attempt": state["attempt"],
    })
    return {
        "human_decision": decision,
        "status": "merged" if decision == "merge" else "rejected",
    }


# --- Conditional edge fonksiyonları: ROTA kararları ---------------------------
def after_quality_gate(state: GraphState) -> str:
    if state["quality_ok"]:
        return "security_gate"
    if state["attempt"] >= state["max_attempts"]:
        return "escalate"          # 3-deneme sınırı: insana yükselt
    return "implementer"           # agent'a geri: düzelt ve tekrar dene


def after_security_gate(state: GraphState) -> str:
    return "reviewer" if state["security_ok"] else "escalate"


def after_human(state: GraphState) -> str:
    return END                     # merged veya rejected: akış biter


def escalate_node(state: GraphState) -> dict:
    return {"status": "escalated"}


# --- Grafın inşası --------------------------------------------------------------
def build_graph(checkpointer) -> StateGraph:
    graph = StateGraph(GraphState)

    graph.add_node("planner", planner_node)
    graph.add_node("implementer", implementer_node)
    graph.add_node("quality_gate", quality_gate_node)
    graph.add_node("security_gate", security_gate_node)
    graph.add_node("reviewer", reviewer_node)
    graph.add_node("human_approval", human_approval_node)
    graph.add_node("escalate", escalate_node)

    graph.add_edge(START, "planner")
    graph.add_edge("planner", "implementer")
    graph.add_edge("implementer", "quality_gate")
    graph.add_edge("reviewer", "human_approval")
    graph.add_edge("escalate", END)

    graph.add_conditional_edges("quality_gate", after_quality_gate)
    graph.add_conditional_edges("security_gate", after_security_gate)
    graph.add_conditional_edges("human_approval", after_human)

    return graph.compile(checkpointer=checkpointer)


def sqlite_checkpointer() -> SqliteSaver:
    """live modu: durum diske yazılır → süreçler arası devam edebilirlik."""
    DB_PATH.parent.mkdir(exist_ok=True)
    conn = sqlite3.connect(str(DB_PATH), check_same_thread=False)
    return SqliteSaver(conn)


def slugify(text: str) -> str:
    s = text.lower().encode("ascii", "ignore").decode()
    return re.sub(r"[^a-z0-9]+", "-", s).strip("-")[:40]


def pending_interrupt(state_snapshot) -> dict | None:
    """Durmuş grafın bekleyen interrupt yükünü (payload) döndürür."""
    for task in state_snapshot.tasks:
        if task.interrupts:
            return task.interrupts[0].value
    return None


def print_reports(values: dict) -> None:
    print(f"  Deneme  : {values.get('attempt')}")
    qr = (values.get("quality_report") or "—").splitlines()
    sr = (values.get("security_report") or "—").splitlines()
    rr = (values.get("review_report") or "—").splitlines()
    print(f"  Quality : {qr[0] if qr else '—'}")
    print(f"  Security: {sr[0] if sr else '—'}")
    print(f"  Reviewer: {rr[0] if rr else '—'}")


# --- sim modu: tek süreçte uçtan uca (Faz 2) ------------------------------------
def run_demo(goal: str, decision: str = "merge") -> None:
    app = build_graph(MemorySaver())
    config = {"configurable": {"thread_id": "demo-1"}}

    print(f"\n=== HEDEF (sim): {goal} ===\n")
    result = app.invoke({"goal": goal, "max_attempts": 3}, config=config)

    print("— Sistem insan onayında DURDU —")
    print_reports(result)

    print(f"\n— İnsan kararı: {decision} —")
    final = app.invoke(Command(resume=decision), config=config)
    print(f"\n=== SON DURUM: {final['status']} ===")


# --- live modu, adım 1: hedef ver, delegasyon interrupt'ında dur -----------------
def run_live(goal: str) -> None:
    app = build_graph(sqlite_checkpointer())
    thread = "live-" + slugify(goal)
    config = {"configurable": {"thread_id": thread}}

    print(f"\n=== CANLI GÖREV: {goal} ===")
    print(f"thread_id: {thread}\n")

    app.invoke({"goal": goal, "mode": "live", "max_attempts": 3}, config=config)

    snapshot = app.get_state(config)
    intr = pending_interrupt(snapshot)
    if intr and intr.get("type") == "delegate":
        pkg = intr["package"]
        print("— Sistem DELEGASYON noktasında DURDU (implementer) —")
        print(f"  Uzman agent : {pkg['agent']}")
        print(f"  Görev paketi: engineering-graph/checkpoints/task-package.json")
        print(f"  Kanıt dosyası: {pkg['evidence_file']}")
        print("\nŞimdi uzman agent kodu yazar ve kanıtı bırakır.")
        print(f"Sonra: ./.venv/bin/python orchestrator.py resume {thread} implemented")
    else:
        print(f"=== SON DURUM: {snapshot.values.get('status')} ===")


# --- live modu, adım 2+: kaldığı yerden devam ------------------------------------
def run_resume(thread: str, decision: str) -> None:
    app = build_graph(sqlite_checkpointer())
    config = {"configurable": {"thread_id": thread}}

    print(f"\n=== RESUME: {thread}  ←  '{decision}' ===\n")
    app.invoke(Command(resume=decision), config=config)

    snapshot = app.get_state(config)
    intr = pending_interrupt(snapshot)

    if intr and intr.get("type") == "delegate":
        print("— Quality gate KIRMIZI döndü, görev agent'a GERİ gönderildi —")
        print_reports(snapshot.values)
        print(f"\nAgent düzeltip kanıtı günceller, sonra:")
        print(f"./.venv/bin/python orchestrator.py resume {thread} implemented")
    elif intr and intr.get("type") == "approve":
        print("— Sistem İNSAN ONAYINDA DURDU —")
        print_reports(snapshot.values)
        print(f"\nKarar: ./.venv/bin/python orchestrator.py resume {thread} merge   (veya reject)")
    else:
        print(f"=== SON DURUM: {snapshot.values.get('status')} ===")


if __name__ == "__main__":
    args = sys.argv[1:]
    if args and args[0] == "live":
        run_live(args[1] if len(args) > 1 else "Backend Maven iskeleti")
    elif args and args[0] == "resume":
        run_resume(args[1], args[2] if len(args) > 2 else "merge")
    else:
        run_demo(args[0] if args else "DLQ retry mekanizması ekle",
                 args[1] if len(args) > 1 else "merge")
