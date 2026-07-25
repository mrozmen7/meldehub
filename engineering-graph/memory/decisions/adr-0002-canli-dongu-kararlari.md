# ADR 0002 — Canlı döngü nasıl çalışır? (Faz 3 kararları)

- **Durum (Status):** Kabul edildi
- **Tarih:** 2026-07-25

## Bağlam (Context)

Faz 2'de graf uçtan uca çalışıyordu ama simülasyondu: implementer kod yazıyormuş
gibi yapıyor, gate'ler sonucu tahmin ediyordu. Sistemin gerçek iş üretmesi için
dört soruya cevap gerekiyordu:

1. Graf, kod yazma işini (dakikalar sürebilir) nasıl devreder ve bekler?
2. Agent'ın ürettiği kodun çalıştığı nasıl doğrulanır?
3. Doğrulama hangi mekanizmayla yapılır ki CI'a taşınabilsin?
4. Graf, ayrı komut çalıştırmaları arasında durumunu nasıl korur?

## Karar (Decision)

1. **Delegasyon interrupt'i:** Implementer, live modda görev paketini
   (`checkpoints/task-package.json`) yazar ve `interrupt()` ile grafiği dondurur.
   Uzman agent kodu dışarıda yazar, kanıtı `checkpoints/evidence.md` dosyasına
   bırakır; insan `resume <thread> implemented` ile grafiği uyandırır.
2. **Gate'ler shell script'idir:** Quality gate `scripts/run-quality-checks.sh`,
   security gate `scripts/security-scan.sh` çalıştırır ve **exit code** okur
   (0 = yeşil). Agent'ın sözü değil, komutun çıkış kodu geçerlidir.
3. **Live modda SqliteSaver:** Graf durumu `checkpoints/graph.db` dosyasına
   yazılır; süreç kapansa bile aynı `thread_id` ile devam edilir.
   Sim modunda MemorySaver yeterlidir (tek süreç).
4. **Runtime artefaktları repo dışı:** `graph.db`, `task-package.json`,
   `evidence.md` `.gitignore`'dadır; repo sadece kalıcı değer taşır.

## Sonuçlar (Consequences)

- (+) Hiçbir kod, gerçek testler geçmeden ve insan onaylamadan merge olamaz
- (+) Gate script'leri elle de çalıştırılabilir → CI'a birebir taşınır
- (+) Graf günler sonra bile kaldığı yerden devam edebilir
- (−) Resume adımı insan disiplinine bağlıdır (unutulursa görev bekler)
- (−) Implementer henüz kural tabanlıdır; LLM API anahtarı eklenirse
  tam otonom delegasyon mümkün olur

## Alternatifler

- Tek büyük prompt ile "yaz + test et + commit'le" → reddedildi: kanıt yok,
  durum yok, hata sınırı yok
- Implementer'ın graf İÇİNDE senkron çalışması → reddedildi: uzun işlerde
  süreç kilitlenir; interrupt + resume daha sağlam
