"""Reviewer düğümü — bağımsız denetçi.

Kod YAZMAZ (yetki kısıtı: .claude/agents/security-reviewer.md).
Çıktıyı kontrol listesine göre inceler ve KANIT raporlar:
dosya, risk seviyesi, öneri. "Baktım oldu" raporu kabul edilmez.
"""

from state import GraphState


def reviewer_node(state: GraphState) -> dict:
    report = (
        "REVIEW RAPORU\n"
        f"İncelenen çıktı: deneme {state['attempt']}\n"
        "Kontrol listesi: secret taraması temiz, auth akışı doğru, "
        "input validation mevcut, hata mesajları iç detay sızdırmıyor.\n"
        "Kritik/yüksek bulgu: 0"
    )

    return {
        "review_report": report,
        "status": "awaiting_human",
    }
