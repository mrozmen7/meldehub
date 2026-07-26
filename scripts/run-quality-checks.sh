#!/usr/bin/env bash
# Quality gate — GERÇEK kontrolleri çalıştırır (simülasyon değil).
# Çıkış kodu 0 = YEŞİL, başka her şey = KIRMIZI.
# Gate karar vermez, ölçer: testler ve derleme gerçekten geçiyor mu?
set -uo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
fail=0

echo "== Quality Gate: kontroller başlıyor =="

if [ -f "$REPO_ROOT/backend/pom.xml" ]; then
  # clean ZORUNLU: incremental build, başarısız derlemelerden sonra
  # "değişiklik yok" sanıp yeniden derlemeyi atlayabilir → false green (ADR-0003)
  # verify ZORUNLU: test + JaCoCo coverage eşiği birlikte denetlenir (ADR-0004)
  echo "-- Backend: mvn -q -B clean verify (test + coverage eşiği)"
  (cd "$REPO_ROOT/backend" && mvn -q -B clean verify) || fail=1
else
  echo "-- Backend: pom.xml yok, atlanıyor"
fi

if [ -f "$REPO_ROOT/frontend/package.json" ]; then
  # npm ci ZORUNLU: package-lock.json'dan birebir kurulum — "bende çalışıyordu"yu önler
  echo "-- Frontend: npm ci + build + unit testler (Vitest, headless)"
  (cd "$REPO_ROOT/frontend" && npm ci --no-audit --no-fund && npm run build && npm test -- --watch=false) || fail=1
else
  echo "-- Frontend: package.json yok, atlanıyor"
fi

if [ "$fail" -eq 0 ]; then
  echo "== Quality Gate sonucu: YESIL =="
else
  echo "== Quality Gate sonucu: KIRMIZI =="
fi
exit "$fail"
