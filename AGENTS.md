# AGENTS.md — MeldeHub Mühendislik Kuralları

Bu dosya, bu repoda çalışan HER agent'ın (ve insanın) uyması gereken kuralları tanımlar.
Agent bir göreve başlamadan ÖNCE bu dosyayı okur.

## 1. Spec-first (Şartname önce)
- Şartnamesiz (spec'siz) kod yazılmaz. Her görev: amaç, kapsam, kabul kriteri içerir.
- Kabul kriteri yoksa görev tanımsızdır → Planner'a geri gönderilir.

## 2. İzolasyon
- Hiçbir agent doğrudan `main` dalına yazmaz.
- Her görev kendi git worktree'sinde ve kendi branch'inde yapılır.

## 3. Kalite kapıları (Quality Gates)
- Merge öncesi zorunlu: derleme + testler yeşil + security gate temiz.
- İnsan onayı bile gate'i atlatamaz. Gate kırmızıysa merge yok, istisna yok.

## 4. Hafıza disiplini (Repository Memory)
- Agent göreve başlamadan önce `engineering-graph/memory/` altını okur.
- Mimari karar veren herkes, kararı ADR olarak `memory/decisions/` altına yazar.
- ADR olmayan mimari karar "alınmamış" sayılır.

## 5. Deneme sınırı
- Bir görev aynı hatada 3 kez başarısız olursa otomatik retry durur,
  görev insan incelemesine yükseltilir. Sonsuz döngü yasaktır.

## 6. Açıklanabilirlik
- Her kod, yazan agent tarafından "neden böyle?" sorusuna cevap verebilir olmalı.
- Cevaplanamayan kod, review'dan geçemez.
