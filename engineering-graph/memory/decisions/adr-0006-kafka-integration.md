# ADR 0006 — Kafka entegrasyonu: event sözleşmesi, DLQ, hata politikası

- **Durum (Status):** Kabul edildi
- **Tarih:** 2026-07-26

## Bağlam (Context)

CASE-142: Vaka yaratılınca yönlendirme (routing) asenkron yapılmalı.
Sorular: topic'e ne basılacak, consumer hatası ne olacak, Kafka
çökerse REST API ne yapacak?

## Karar (Decision)

1. **Ayrı event DTO'su (`CaseCreatedEvent`):** Entity topic'e basılmaz;
   minimal sözleşme (caseId, category, location, occurredAt).
2. **Key = caseId:** Aynı vakanın event'leri aynı partition'a → sıra garantisi.
3. **DLQ politikası:** 2 retry (1 sn sabit backoff) sonrası mesaj
   `<topic>.DLT`'ye. Zehirli mesaj kuyruğu kilitlemez.
4. **Kafka çökerse REST patlamaz:** Producer hatası loglanır, vaka
   kaydedilmiş kalır. Bilinçli trade-off: event kaybı kabul edilebilir,
   kayıt kaybı değil. Kalıcı çözüm **Outbox pattern** → Faz 11 backlog.
5. **Topic adı config'den:** `app.kafka.case-created-topic`; koda gömülmez.

## Sonuçlar (Consequences)

- (+) Routing/notification gibi yeni consumer'lar REST'i değiştirmeden eklenir
- (+) Zehirli mesaj operasyonel görünür (DLT), sistemi durdurmaz
- (−) Kafka kesintisinde event kaybı sessiz (log'da kalır) → Outbox ile kapanacak
- (−) Event sözleşmesi versiyonlama gerektirir (şimdilik v1, tek consumer)

## Alternatifler

- Senkron çağrı (routing'i REST içinde yap) → reddedildi: coupling,
  yavaşlık, tek hata noktası
- Entity'yi topic'e basmak → reddedildi: iç şema dışarı sızar
- Outbox pattern şimdi → ertelendi: iskelet aşamada kapsam dışı,
  backlog'a yazıldı (bilinçli borç, ADR'de izi var)
