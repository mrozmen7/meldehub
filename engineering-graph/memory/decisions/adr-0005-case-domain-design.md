# ADR 0005 — Case domain tasarımı: zengin model + durum makinesi

- **Durum (Status):** Kabul edildi
- **Tarih:** 2026-07-26

## Bağlam (Context)

MeldeHub'ın kalbi vaka yaşam döngüsü: NEW → TRIAGED → IN_PROGRESS →
RESOLVED → CLOSED. Soru: geçiş kuralları nerede yaşamalı ve testler
hangi veritabanıyla koşmalı?

## Karar (Decision)

1. **Zengin domain modeli:** Geçiş kuralı entity'dedir (`Case.changeStatus`).
   Geçersiz geçiş `InvalidStatusTransitionException` fırlatır → HTTP 409.
   Kural hangi katman çağırırsa çağırsın atlatılamaz.
2. **Durum makinesi enum'da:** `CaseStatus.canTransitionTo` — tek bakışta
   tüm yaşam döngüsü; CLOSED'den çıkış yok.
3. **Runtime PostgreSQL, test H2:** Testler dış bağımlılık istemez;
   CI sıfır kurulumla yeşil kalır. Ana config env değişkenlidir
   (`DB_URL`, `DB_USER`, `DB_PASSWORD`).
4. **Şema yönetimi geçici olarak `ddl-auto: update`:** Flyway migration
   Faz 11'de (production fazı).
5. **API, DTO ile konuşur:** Entity dışarı açılmaz; giriş `CaseCreateRequest`
   (+ Bean Validation), çıkış `CaseResponse`.

## Sonuçlar (Consequences)

- (+) İş kuralı tek yerde, test edilebilir, atlatılamaz
- (+) Testler her ortamda koşar (H2); CI bağımsız
- (−) H2/PostgreSQL davranış farkları teorik risk → Faz 11'de
  Testcontainers ile PostgreSQL entegrasyon testi değerlendirilir
- (−) ddl-auto: update production'a taşınmamalı → Faz 11 görevi

## Alternatifler

- Anemik model + serviste kural → reddedildi: kural dağılır, atlatılabilir
- Testlerde de PostgreSQL (Testcontainers) → şimdilik reddedildi:
  iskelet aşamada hız öncelikli; Faz 11'de tekrar değerlendirilir
