"""Security gate — secret + açık taraması.

Kritik bulgu varsa iş DURUR ve insana yükselir (escalated) —
agent'a geri dönmez, çünkü güvenlik bulgusu "düzelt-tekrar dene"
değil "dur ve bak" gerektirir. Gerçek sürümde gitleaks +
OWASP dependency-check çalıştırır.
"""

from state import GraphState


def security_gate_node(state: GraphState) -> dict:
    # Simülasyon: tarama temiz.
    passed = True

    return {
        "security_ok": passed,
        "security_report": "SECURITY GATE: YEŞİL — secret yok, bilinen açık yok.",
        "status": "review",
    }
