# ADR 0003 — Gate'ler neden clean build çalıştırır?

- **Durum (Status):** Kabul edildi
- **Tarih:** 2026-07-25

## Bağlam (Context)

Faz 5 failure drill'i sırasında quality gate, kaynak kodda derleme hatası
(`return pong;` — tanımsız değişken) dururken **YEŞİL** döndü. Kök neden:
`mvn test` komutu `clean` olmadan çalıştığında Maven'ın incremental
derlemesi, önceki başarısız derlemelerin bıraktığı durum nedeniyle
yeniden derlemeyi atlayabiliyor; `target/` altındaki eski artefaktlarla
build "başarılı" görünüyor. Buna **false green (yanıltıcı yeşil)** denir.

Gate'in tek doğruluk kaynağı exit code'dur — exit code yanlışsa
sistemin tüm güven modeli çöker.

## Karar (Decision)

Quality gate script'i her zaman **clean build** çalıştırır:
`mvn -q -B clean test`. `target/` her kontrolde sıfırdan kurulur;
hiçbir eski artefakt sonucu etkileyemez.

Aynı drill'de insan onay noktası, false green'i fark edip görevi
**reject** ederek ikinci savunma hattının çalıştığını kanıtlamıştır.

## Sonuçlar (Consequences)

- (+) False green imkânsız: her kontrol sıfırdan, tekrarlanabilir derleme
- (+) CI ortamıyla birebir aynı davranış (CI zaten temiz ortamda çalışır)
- (−) Her gate çalıştırması birkaç saniye daha yavaş (tam derleme)
  → kabul edilebilir: doğruluk > hız

## Alternatifler

- `mvn test` (clean'siz) → reddedildi: false green riski (bu drill'de yaşandı)
- Testlere controller'ı doğrudan referans ekleyerek NoClassDefFoundError
  zorlamak → reddedildi: belirtiyi tedavi eder, hastalığı değil
