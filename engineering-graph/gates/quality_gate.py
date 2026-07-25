"""Quality gate — test + derleme kontrolü.

Gate'ler KARAR VERMEZ, ÖLÇER: koşul sağlanıyor mu, sağlanmıyor mu?

İki mod:
  sim  → ilk denemeyi bilerek başarısız sayar (retry döngüsünü öğretmek için)
  live → scripts/run-quality-checks.sh betiğini GERÇEKTEN çalıştırır
         ve işletim sisteminin çıkış kodunu (exit code) okur:
         0 = YEŞİL, başka her şey = KIRMIZI.
"""

import subprocess
from pathlib import Path

from state import GraphState

REPO_ROOT = Path(__file__).resolve().parents[2]   # meldehub/
SCRIPT = REPO_ROOT / "scripts" / "run-quality-checks.sh"


def quality_gate_node(state: GraphState) -> dict:
    # --- sim modu: 1. deneme kırmızı, 2. denemeden itibaren yeşil ------
    if state.get("mode", "sim") == "sim":
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

    # --- live modu: gerçek komut, gerçek çıkış kodu ----------------------
    proc = subprocess.run(
        ["bash", str(SCRIPT)],
        cwd=REPO_ROOT,
        capture_output=True,
        text=True,
        timeout=280,
    )
    passed = proc.returncode == 0
    tail = "\n".join((proc.stdout + proc.stderr).splitlines()[-12:])
    report = (
        f"QUALITY GATE: {'YEŞİL' if passed else 'KIRMIZI'} — "
        f"run-quality-checks.sh çıkış kodu {proc.returncode}\n{tail}"
    )
    return {
        "quality_ok": passed,
        "quality_report": report,
        "status": "security_check" if passed else "implementing",
    }
