"""Planner düğümü — hedefi şartnameye (spec) çevirir.

Planner KOD YAZMAZ. Tek işi: hedefi, Implementer'ın çalışabileceği
net bir spec'e dönüştürmek. AGENTS.md kural 1: kabul kriteri olmayan
görev tanımsızdır — bu yüzden spec her zaman 3 bölümden oluşur.

Not: Bu iskelet sürüm kural tabanlıdır. LLM anahtarı eklendiğinde
spec üretimi modele devredilir; düğüm imzası değişmez.
"""

from state import GraphState


def planner_node(state: GraphState) -> dict:
    goal = state["goal"]

    spec = f"""SPEC — {goal}
Amaç: {goal}
Kapsam: Yalnızca bu hedef için gereken değişiklikler; kapsam dışı istekler Planner'a geri döner.
Kabul kriterleri:
  1. İlgili testler yeşil (mvn test / npm test).
  2. Security gate temiz (secret yok, açık yok).
  3. Mimari karar varsa ADR taslağı memory/decisions altında.
"""

    return {
        "spec": spec,
        "attempt": 0,
        "max_attempts": 3,
        "status": "implementing",
    }
