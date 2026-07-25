"""Engineering Graph — paylaşılan durum (State) tanımı.

State = grafiğin "hafızası". Her düğüm (node) bu durumu okur ve
güncelleyerek bir sonraki düğüme aktarır. Düğümler birbirini tanımaz;
tek ortak noktaları bu durum nesnesidir.
"""

from typing import TypedDict, Literal


class GraphState(TypedDict, total=False):
    # --- Girdi ---
    goal: str                      # İnsanın verdiği hedef ("DLQ retry ekle")
    mode: Literal["sim", "live"]   # sim: simülasyon | live: gerçek delegasyon + gerçek gate'ler
    spec: str                      # Planner'ın ürettiği şartname

    # --- Çalışma alanı ---
    artifact: str                  # Implementer'ın ürettiği çıktı (kod özeti)
    attempt: int                   # Kaçıncı deneme (3-deneme sınırı için)
    max_attempts: int              # AGENTS.md kural 5: sonsuz döngü yok

    # --- Gate sonuçları ---
    quality_ok: bool               # Quality gate geçti mi?
    quality_report: str            # Gate'in kanıt raporu
    security_ok: bool              # Security gate geçti mi?
    security_report: str

    # --- Reviewer ---
    review_report: str             # Bağımsız denetçinin raporu

    # --- Akış kontrolü ---
    status: Literal[
        "planning", "implementing", "quality_check",
        "security_check", "review", "awaiting_human",
        "escalated", "merged", "rejected",
    ]
    human_decision: str            # İnsan onayı: "merge" veya "reject"
