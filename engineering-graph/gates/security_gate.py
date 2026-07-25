"""Security gate — secret + açık taraması.

Kritik bulgu varsa iş DURUR ve insana yükselir (escalated) —
agent'a geri dönmez, çünkü güvenlik bulgusu "düzelt-tekrar dene"
değil "dur ve bak" gerektirir.

İki mod:
  sim  → taramayı temiz sayar
  live → scripts/security-scan.sh betiğini GERÇEKTEN çalıştırır
         (gitleaks'in minimal karşılığı). Çıkış kodu 0 = temiz.
"""

import subprocess
from pathlib import Path

from state import GraphState

REPO_ROOT = Path(__file__).resolve().parents[2]   # meldehub/
SCRIPT = REPO_ROOT / "scripts" / "security-scan.sh"


def security_gate_node(state: GraphState) -> dict:
    if state.get("mode", "sim") == "sim":
        return {
            "security_ok": True,
            "security_report": "SECURITY GATE: YEŞİL — secret yok, bilinen açık yok.",
            "status": "review",
        }

    # --- live modu: gerçek tarama ---------------------------------------
    proc = subprocess.run(
        ["bash", str(SCRIPT)],
        cwd=REPO_ROOT,
        capture_output=True,
        text=True,
        timeout=120,
    )
    passed = proc.returncode == 0
    tail = "\n".join((proc.stdout + proc.stderr).splitlines()[-12:])
    report = (
        f"SECURITY GATE: {'YEŞİL' if passed else 'KIRMIZI'} — "
        f"security-scan.sh çıkış kodu {proc.returncode}\n{tail}"
    )
    return {
        "security_ok": passed,
        "security_report": report,
        "status": "review" if passed else "escalated",
    }
