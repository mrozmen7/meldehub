# ADR 0001 — Bu sistem neden var?

- **Durum (Status):** Kabul edildi
- **Tarih:** 2026-07-25

## Bağlam (Context)

MeldeHub (belediye vaka yönlendirme platformu) geliştirilecek. Geliştirme sürecinin
kendisi de bir mühendislik ürünü olacak: uzman AI agent'ları, izole çalışma alanları,
otomatik kalite kapıları ve insan onay noktaları ile.

Klasik yaklaşım (tek chat penceresinde AI ile kod yazmak) şu sorunları üretir:
- Kararların "neden"i kaybolur (oturum kapanınca hafıza gider)
- Kalite kontrolü insanın dikkatine bağlıdır (dikkat = ölçeklenemez)
- Paralel işlerde çakışma olur

## Karar (Decision)

Geliştirme sürecini bir **Engineering Graph** olarak kodlayacağız:
görevler bir graf üzerinde uzman agent'lar arasında akar; her geçişte
otomatik kontroller (gate) çalışır; kritik noktalarda insan onayı zorunludur;
mimari kararlar repository memory'ye (ADR) geri yazılır.

## Sonuçlar (Consequences)

- (+) Her karar izlenebilir, her kod savunulabilir
- (+) Kalite, dikkate değil sisteme bağlı
- (−) Başlangıç kurulum maliyeti var (iskelet + agent tanımları + gate'ler)
- (−) Küçük tek satırlık değişikliklerde bile süreç disiplini gerekir

## Alternatifler

- Doğrudan AI chat ile geliştirme → reddedildi: hafıza ve denetim yok
- Tam otonom agent (insan onaysız) → reddedildi: kurumsal ortamda kabul edilemez risk
