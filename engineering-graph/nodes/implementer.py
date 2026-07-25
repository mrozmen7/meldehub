"""Implementer düğümü — spec'i çıktıya (artifact) çevirir.

Gerçek sistemde bu düğüm, görevi ilgili uzman agent'a
(.claude/agents/ altındaki tanımlar) izole worktree'de devreder.
İskelet sürümde üretimi simüle eder ama akış gerçektir:
her çağrıda deneme sayacı artar — 3-deneme sınırı burada uygulanır.
"""

from state import GraphState


def implementer_node(state: GraphState) -> dict:
    attempt = state.get("attempt", 0) + 1

    artifact = (
        f"[Deneme {attempt}] Spec uygulandı: {state['spec'].splitlines()[0]}\n"
        f"Üretilen değişiklik özeti ve test çıktıları burada yer alır."
    )

    return {
        "artifact": artifact,
        "attempt": attempt,
        "status": "quality_check",
    }
