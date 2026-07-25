#!/usr/bin/env bash
# Security gate — secret taraması (gitleaks'in minimal karşılığı).
# Kritik bulgu varsa çıkış kodu 1: iş DURUR, agent'a geri dönmez.
# (Güvenlik bulgusu "düzelt-tekrar dene" değil "dur ve bak" gerektirir.)
set -uo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

echo "== Security Gate: secret taraması =="

if grep -rEnI \
  -e 'BEGIN [A-Z ]*PRIVATE KEY' \
  -e '(password|secret|api[_-]?key)\s*[:=]\s*"[^"]{6,}"' \
  --exclude-dir=.git \
  --exclude-dir=.venv \
  --exclude-dir=node_modules \
  --exclude-dir=target \
  --exclude='security-scan.sh' \
  "$REPO_ROOT"; then
  echo "== SECURITY: KIRMIZI — olasi secret bulundu, is durduruldu =="
  exit 1
fi

echo "== SECURITY: YESIL — secret izi yok =="
exit 0
