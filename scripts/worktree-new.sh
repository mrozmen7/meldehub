#!/usr/bin/env bash
# Worktree aç — bir görev için ANA ÇALIŞMA ALANINI BOZMADAN
# izole bir kopya + yeni branch oluşturur.
# Kullanım: scripts/worktree-new.sh <gorev-no>
# (Faz 3'te hazırlanıyor; tam izolasyon disiplini Faz 6'da.)
set -euo pipefail

N="${1:?kullanim: scripts/worktree-new.sh <gorev-no>}"

git worktree add "../meldehub-gorev-$N" -b "gorev/$N"
echo "Worktree hazir: ../meldehub-gorev-$N  (branch: gorev/$N)"
echo "Agent bu klasorde calisir; ana dal temiz kalir."
