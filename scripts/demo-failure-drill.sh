#!/usr/bin/env bash
# demo-failure-drill.sh — Canlı mülakat demosu (~5 dakika)
#
# İzleyiciye gösterir: agent hata yapar → gate yakalar → retry (feedback ile)
# → 3. başarısızlıkta sistem KENDİNİ durdurur (escalate) → insan devralır.
#
# Kullanım:
#   scripts/demo-failure-drill.sh          her adımda Enter bekler (anlatım için)
#   scripts/demo-failure-drill.sh --auto   duraksamadan koşar (prova/test için)
#
# Script geçici olarak bozuk bir Java dosyası yazar; ÇIKIŞTA HER ZAMAN temizler
# (Ctrl+C ile kesilse bile — trap EXIT).
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
EG="$REPO_ROOT/engineering-graph"
API_DIR="$REPO_ROOT/backend/src/main/java/ch/meldehub/api"
PY="$EG/.venv/bin/python"
GOAL="Demo drill $(date +%H%M%S)"
# thread_id'yi orchestrator'ın KENDİ slugify fonksiyonuyla üret (bash sed ile
# tahmin etmeye çalışma — uyuşmazlık resume'yi bozar):
THREAD=$(cd "$EG" && "$PY" -c "from orchestrator import slugify; print('live-' + slugify('$GOAL'))")
AUTO="${1:-}"

pause() { [ "$AUTO" = "--auto" ] || { echo; read -r -p "  ⏸  (anlatım noktası — devam için Enter)"; echo; }; }

cleanup() {
  rm -f "$API_DIR/BrokenDemo.java" "$EG/checkpoints/evidence.md" "$EG/checkpoints/task-package.json"
}
trap cleanup EXIT

resume() { (cd "$EG" && "$PY" orchestrator.py resume "$THREAD" implemented); }

evidence() {
  cat > "$EG/checkpoints/evidence.md" <<EOF
# Kanıt — Demo görev (agent denemesi $1/3)
Agent kodu yazdığını söylüyor. Sistem söze değil, gate çıkış koduna bakar.
EOF
}

echo "════════════════════════════════════════════════════════"
echo "  DEMO: AI agent hata yaparsa sistem ne yapar?"
echo "════════════════════════════════════════════════════════"
echo
echo "▶ ADIM 1/7 — Görev veriliyor (graf delegasyon noktasında duracak):"
(cd "$EG" && "$PY" orchestrator.py live "$GOAL")
pause

echo "▶ ADIM 2/7 — Agent 1. denemeyi yapıyor... (bilerek hatalı kod)"
cat > "$API_DIR/BrokenDemo.java" <<'EOF'
package ch.meldehub.api;

public class BrokenDemo {
    public String deneme() {
        return "bir"   // agent hatası 1: noktalı virgül unutulmuş
    }
}
EOF
evidence 1
echo "▶ ADIM 3/7 — Resume: quality gate GERÇEK 'mvn clean verify' çalıştırıyor..."
resume
pause

echo "▶ ADIM 4/7 — Görev, hata raporuyla (quality_feedback) agent'a geri döndü."
echo "              Agent 2. denemeyi yapıyor... (yine hatalı)"
cat > "$API_DIR/BrokenDemo.java" <<'EOF'
package ch.meldehub.api;

public class BrokenDemo {
    public String deneme() {
        return 42;   // agent hatası 2: String metotta int dönüşü
    }
}
EOF
evidence 2
echo "▶ ADIM 5/7 — Resume: ikinci gerçek kontrol..."
resume
pause

echo "▶ ADIM 6/7 — 3. ve SON deneme (sistem sınırı: max 3):"
cat > "$API_DIR/BrokenDemo.java" <<'EOF'
package ch.meldehub.api;

public class BrokenDemo {
    public String deneme() {
        return ucuncu;   // agent hatası 3: tanımsız değişken
    }
}
EOF
evidence 3
resume
pause

echo "▶ ADIM 7/7 — SONUÇ:"
echo "  • 3 kez üst üste başarısız olan görev için 4. deneme YAPILMADI."
echo "  • Sistem kendini durdurdu: status = escalated (insana devir)."
echo "  • Repo temiz bırakıldı (bozuk demo dosyası silindi)."
echo
echo "  Hikâyenin tamamı: docs/failure-drill.md — gate'in kendi zaafını"
echo "  (false green) bile bu tatbikatla bulduk: memory/decisions/adr-0003."
echo "════════════════════════════════════════════════════════"
