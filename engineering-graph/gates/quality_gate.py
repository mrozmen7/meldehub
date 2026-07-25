"""Quality gate — test + derleme kontrolü.

Gate'ler KARAR VERMEZ, ÖLÇER: koşul sağlanıyor mu, sağlanmıyor mu?
İskelet sürümde ilk denemeyi bilerek başarısız sayar ki retry
döngüsü gözlemlenebilsin. Gerçek sürümde bu düğüm
scripts/quality-gate.sh çalıştırır ve çıkış kodunu okur.
"""

from state import GraphState


def quality_gate_node(state: GraphState) -> dict:
    # Simülasyon: 1. deneme kırmızı, 2. denemeden itibaren yeşil.
    passed = state.get("attempt", 0) >= 2

    report = (
        "QUALITY GATE: YEŞİL — testler geçti, derleme temiz."
        if passed
        else "QUALITY GATE: KIRMIZI — 2 test başarısız (örnek). Agent'a geri gönderildi."
    )

    return {
        "quality_ok": passed,
        "quality_report": report,
        "status": "security_check" if passed else "implementing",
    }
