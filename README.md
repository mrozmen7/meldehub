# MeldeHub

Belediye vaka yönlendirme & entegrasyon platformu + onu inşa eden Engineering Graph sistemi.

## Bu repo iki şeydir

1. **Ürün:** Heterojen kaynaklardan (web formu, legacy API, CSV import) gelen vatandaş
   ihbarlarını Kafka üzerinden normalize edip doğru departmana yönlendiren sistem.
2. **Süreç:** Bu ürünü inşa eden agentic mühendislik sistemi — uzman agent'lar,
   izole worktree'ler, otomatik quality/security gate'leri, insan onay noktaları
   ve repository memory (ADR tabanlı kalıcı hafıza).

## Yapı

Detay için: `engineering-graph/memory/code-map.md`
Kurallar için: `AGENTS.md`
Kararlar için: `engineering-graph/memory/decisions/`
