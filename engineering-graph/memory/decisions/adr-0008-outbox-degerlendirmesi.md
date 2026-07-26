# ADR 0008 — Outbox pattern değerlendirmesi: ERTELENDİ

- **Durum (Status):** Kabul edildi (erteleme kararı)
- **Tarih:** 2026-07-26

## Bağlam (Context)

ADR-0006'da bilinçli bir trade-off kaydedildi: Kafka çökerse producer hatası
loglanır, vaka kaydedilmiş kalır ama **event sessizce kaybolur** ("event
basılamazsa kaybolur" riski). CASE-187 kapsamında production paketi
hazırlanırken bu borç yeniden değerlendirildi: Outbox pattern şimdi
uygulanmalı mı?

## Karar (Decision)

**ERTELENDİ.** Outbox pattern şu an uygulanmıyor; backlog'da bilinçli borç
olarak kalıyor. Gerekçe: iskelet aşamada tek consumer (routing log'luyor),
event kaybının iş etkisi düşük ve log'dan izlenebilir; Outbox'un maliyeti
(ek tablo, relay süreci, idempotency sözleşmesi) bugünkü riske oranla yüksek.

**Tetik koşulları** — aşağıdakilerden biri gerçekleşirse karar gözden
geçirilir ve Outbox uygulanır:

1. Belediye SLA'sı event kaybını kabul edilemez bulursa (ör. yönlendirme
   gecikmesi/kayıbı için denetim sorusu gelirse)
2. Audit zorunluluğu gelirse (her vakanın birimlere iletildiğinin kanıtı
   istenirse)
3. Kritik yeni consumer eklenirse (SMS/e-posta bildirimi gibi kaybı vatandaşa
   yansıyan bir tüketici)
4. DLQ/retry metrikleri tekrarlayan producer hatası gösterirse (risk teoriden
   pratiğe dönerse)

## Sonuçlar (Consequences)

- (+) Kapsam kontrolü: production paketi Outbox karmaşası olmadan tamamlandı
- (+) Karar ve tetik koşulları yazılı → borç görünür, sürpriz değil
- (−) Kafka kesintisinde event kaybı riski hâlâ açık (log'da kalır); izleme
  görevi operasyonda

## Alternatifler

- **Outbox pattern şimdi** (outbox tablosu + relay ile DB→Kafka atomikliği) →
  ertelendi: yukarıdaki gerekçe; tetik koşulları tanımlandı
- **Retry topic** (producer tarafında yerel yeniden deneme kuyruğu) →
  kısmi çözüm: uygulama çökerse yine kayıp; kalıcılık sağlamaz
- **CDC / Debezium** (WAL üzerinden outbox okuma) → güçlü ama operasyonel
  ağırlık (connector cluster, schema registry) bu ölçekte gereksiz; Outbox
  uygulanırsa relay olarak Debezium doğal adaydır
