#!/usr/bin/env bash
# Quality gate — GERÇEK kontrolleri çalıştırır (simülasyon değil).
# Çıkış kodu 0 = YEŞİL, başka her şey = KIRMIZI.
# Gate karar vermez, ölçer: testler ve derleme gerçekten geçiyor mu?
set -uo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
fail=0

echo "== Quality Gate: kontroller başlıyor =="

if [ -f "$REPO_ROOT/backend/pom.xml" ]; then
  echo "-- Backend: mvn -q -B test"
  (cd "$REPO_ROOT/backend" && mvn -q -B test) || fail=1
else
  echo "-- Backend: pom.xml yok, atlanıyor"
fi

if [ -f "$REPO_ROOT/frontend/package.json" ]; then
  echo "-- Frontend: package.json bulundu (test komutu Faz 10'da bağlanacak)"
else
  echo "-- Frontend: package.json yok, atlanıyor"
fi

if [ "$fail" -eq 0 ]; then
  echo "== Quality Gate sonucu: YESIL =="
else
  echo "== Quality Gate sonucu: KIRMIZI =="
fi
exit "$fail"
