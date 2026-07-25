# Kanıt Şablonu (Evidence Template)

Her uzman agent, görevini bitirince bu şablonu doldurarak
`engineering-graph/checkpoints/evidence.md` dosyasına yazar.
Gate'ler bu dosyaya bakmaz (onlar exit code okur); bu dosya İNSAN içindir.

---

# Kanıt — Görev <no>: <başlık>

**Agent:** <hangi uzman>   **Hedef:** <görev paketindeki goal>

## Üretilenler
<!-- Hangi dosyalar eklendi/değişti? Tek tek liste. -->

## Doğrulama
<!-- Hangi komutları çalıştırdın, sonuç ne oldu?
     Unutma: bağımsız doğrulamayı quality gate tekrar yapar. -->

## Hafıza etkisi  ← AGENTS.md kural 4 gereği ZORUNLU
<!-- - Yeni terim doğdu mu? → glossary.md'ye eklendi mi?
     - Yapı değişti mi (yeni klasör/dosya)? → code-map.md güncellendi mi?
     - Mimari karar verildi mi? → ADR yazıldı mı (memory/decisions/)? -->

## Bilinçli dışarıda bırakılanlar
<!-- Neyi yapmadın, neden, hangi faza ertelendi? -->
