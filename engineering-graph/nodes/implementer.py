"""Implementer düğümü — spec'i çıktıya (artifact) çevirir.

İki mod:
  sim  → üretimi simüle eder (Faz 2 davranışı, akışı öğretmek için)
  live → GERÇEK delegasyon: görev paketi hazırlar, interrupt() ile
         grafiği DONDURUR ve uzman agent'ın (.claude/agents altındaki
         tanımlar) kodu yazmasını bekler. Agent kanıtı
         checkpoints/evidence.md dosyasına bırakır; insan
         `resume <thread> implemented` ile grafiği uyandırır.

Retry'da (attempt > 1) quality gate'in raporu pakete geri beslenir —
agent "neyin kırıldığını" bilerek tekrar dener.
"""

import json
from pathlib import Path

from langgraph.types import interrupt

from state import GraphState

EG_ROOT = Path(__file__).resolve().parents[1]   # engineering-graph/
CHECKPOINTS = EG_ROOT / "checkpoints"


def pick_agent(goal: str) -> str:
    """Hedefe göre uzman agent seçimi (.claude/agents tanımları)."""
    g = goal.lower()
    if any(k in g for k in ("kafka", "dlq", "event", "topic")):
        return "kafka-expert"
    if any(k in g for k in ("angular", "frontend", "ui")):
        return "angular-expert"
    return "spring-architect"


def implementer_node(state: GraphState) -> dict:
    attempt = state.get("attempt", 0) + 1

    # --- sim modu: Faz 2'deki gibi üretimi taklit et -------------------
    if state.get("mode", "sim") == "sim":
        artifact = (
            f"[Deneme {attempt}] Spec uygulandı: {state['spec'].splitlines()[0]}\n"
            f"Üretilen değişiklik özeti ve test çıktıları burada yer alır."
        )
        return {"artifact": artifact, "attempt": attempt, "status": "quality_check"}

    # --- live modu: gerçek delegasyon -----------------------------------
    package = {
        "task_id": f"gorev-{attempt}",
        "agent": pick_agent(state["goal"]),
        "goal": state["goal"],
        "spec": state["spec"],
        "attempt": attempt,
        "evidence_file": "engineering-graph/checkpoints/evidence.md",
        # Retry'da agent'a kırılan testin raporunu geri besle:
        "quality_feedback": state.get("quality_report") if attempt > 1 else None,
    }
    CHECKPOINTS.mkdir(exist_ok=True)
    (CHECKPOINTS / "task-package.json").write_text(
        json.dumps(package, ensure_ascii=False, indent=2)
    )

    # Graf burada DONDURULUR. Uzman agent kodu yazar, kanıtı bırakır,
    # insan resume ile devam ettirir.
    interrupt({"type": "delegate", "package": package})

    evidence_path = CHECKPOINTS / "evidence.md"
    evidence = (
        evidence_path.read_text(encoding="utf-8")
        if evidence_path.exists()
        else "(evidence.md bulunamadı — agent kanıt bırakmadı)"
    )
    return {"artifact": evidence, "attempt": attempt, "status": "quality_check"}
