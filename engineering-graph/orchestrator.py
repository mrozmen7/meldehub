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

Kavramlar (Faz 2):
  State            → GraphState: düğümler arası paylaşılan hafıza
  Node             → bir iş adımı (planner, implementer, gate...)
  Edge             → koşulsuz geçiş (A bitince hep B'ye)
  Conditional Edge → koşullu yönlendirme (gate sonucuna göre rota)
  Command(resume)  → interrupt'tan sonra grafiğe insan kararını taşır
  Checkpointer     → durumu kaydeder; graf durup kaldığı yerden devam eder
"""

import sys

from langgraph.checkpoint.memory import MemorySaver
from langgraph.graph import END, START, StateGraph
from langgraph.types import Command, interrupt

from gates.quality_gate import quality_gate_node
from gates.security_gate import security_gate_node
from nodes.implementer import implementer_node
from nodes.planner import planner_node
from nodes.reviewer import reviewer_node
from state import GraphState


# --- İnsan onay düğümü: sistemin DURDUĞU yer -------------------------------
def human_approval_node(state: GraphState) -> dict:
    """interrupt() çağrısı grafiği burada dondurur.

    İnsan karar verene kadar hiçbir düğüm çalışmaz. Karar,
    Command(resume=...) ile bu noktaya geri beslenir.
    """
    decision = interrupt({
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


# --- Conditional edge fonksiyonları: ROTA kararları -------------------------
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


# --- Grafın inşası ------------------------------------------------------------
def build_graph() -> StateGraph:
    graph = StateGraph(GraphState)

    # Düğümler
    graph.add_node("planner", planner_node)
    graph.add_node("implementer", implementer_node)
    graph.add_node("quality_gate", quality_gate_node)
    graph.add_node("security_gate", security_gate_node)
    graph.add_node("reviewer", reviewer_node)
    graph.add_node("human_approval", human_approval_node)
    graph.add_node("escalate", escalate_node)

    # Koşulsuz kenarlar (edge)
    graph.add_edge(START, "planner")
    graph.add_edge("planner", "implementer")
    graph.add_edge("implementer", "quality_gate")
    graph.add_edge("reviewer", "human_approval")
    graph.add_edge("escalate", END)

    # Koşullu kenarlar (conditional edge): gate sonucuna göre rota
    graph.add_conditional_edges("quality_gate", after_quality_gate)
    graph.add_conditional_edges("security_gate", after_security_gate)
    graph.add_conditional_edges("human_approval", after_human)

    # Checkpointer: interrupt'ta duran graf kaldığı yerden devam edebilsin
    return graph.compile(checkpointer=MemorySaver())


# --- Demo: uçtan uca akış ------------------------------------------------------
def run_demo(goal: str) -> None:
    app = build_graph()
    config = {"configurable": {"thread_id": "demo-1"}}

    print(f"\n=== HEDEF: {goal} ===\n")

    # 1. çalıştırma: human_approval'daki interrupt'a kadar akar
    result = app.invoke({"goal": goal}, config=config)

    print("— Sistem insan onayında DURDU —")
    print(f"  Deneme sayısı : {result['attempt']}")
    print(f"  Quality gate  : {result['quality_report']}")
    print(f"  Security gate : {result['security_report']}")
    print(f"  Reviewer      : {result['review_report'].splitlines()[0]}")

    # 2. insan kararı: resume ile grafiğe geri beslenir
    decision = sys.argv[2] if len(sys.argv) > 2 else "merge"
    print(f"\n— İnsan kararı: {decision} —")
    final = app.invoke(Command(resume=decision), config=config)
    print(f"\n=== SON DURUM: {final['status']} ===")


if __name__ == "__main__":
    run_demo(sys.argv[1] if len(sys.argv) > 1 else "DLQ retry mekanizması ekle")
