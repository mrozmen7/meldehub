# ADR 0004 — Coverage eşiği ve gate'lerin CI'a taşınması

- **Durum (Status):** Kabul edildi — ADR-0003'ü geçersiz kılar (superseded)
- **Tarih:** 2026-07-26

## Bağlam (Context)

ADR-0003 gate'e `clean test` zorunluluğu getirdi ama iki boşluk kaldı:

1. **Test var, ölçü yok:** contextLoads gibi duman testleri "testimiz var"
   hissi verir ama kodun ne kadarının gerçekten sınandığını kimse bilmez.
2. **Gate'ler sadece lokal:** Disipline bağlı kontrol, unutulabilir.
   "Herkes koşar zaten" bir garanti değildir.

## Karar (Decision)

1. Quality gate `mvn clean verify` çalıştırır: testler + JaCoCo coverage
   kontrolü. Satır kapsamı %80 altına düşerse build KIRILIR.
2. Gate'ler CI'a taşınır: `.github/workflows/ci.yml` her push'ta ve PR'da
   AYNI script'leri koşar (`run-quality-checks.sh`, `security-scan.sh`).
   CI ayrı bir dünya kurmaz; lokaldeki kapıların uzaktan tekrarıdır.
3. Bağımlılık açığı taraması OWASP dependency-check plugin'i yerine
   GitHub Dependabot'a bırakılır (NVD indirimi dakikalar sürer;
   Dependabot repo seviyesinde ücretsiz ve sürekli çalışır).

## Sonuçlar (Consequences)

- (+) "Testim var" hissi ölçüye dönüştü: %80 eşiği
- (+) Lokalde unutulan kontrol GitHub'da yakalanır; push/PR kırmızı görünür
- (+) Lokal/CI davranış farkı yok — aynı script, aynı exit code
- (−) Eşik, iskelet aşamasında bilinçli esnek tutuldu (%80); domain
  büyüyünce gözden geçirilmeli

## Alternatifler

- Coverage olmadan devam → reddedildi: duman testi yanılsaması
- OWASP dependency-check Maven plugin → reddedildi: her build'de NVD
  veritabanı indirimi (dakikalar); CI'ı kullanılmaz yavaşlatır
