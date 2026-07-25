# Faz 5 — Failure Drill Raporu (2026-07-25)

Sistemin hata karşısındaki üç davranışı canlı olarak test edildi:
retry, escalate, insan müdahalesi. Drill planlanmamış gerçek bir arıza
(false green) da ortaya çıkardı ve sistemi sertleştirdi.

## Senaryo 1: Retry döngüsü (thread: live-casecontroller-ekle-...)
| Deneme | Kod durumu | Gate | Sistem tepkisi |
|--------|-----------|------|----------------|
| 1 | `;` eksik | KIRMIZI (exit 1) | Görev agent'a geri, `quality_feedback` ile |
| 2 | String'e int dönüş | KIRMIZI (exit 1) | Görev agent'a geri (attempt 3) |
| 3 | Tanımsız değişken | **FALSE GREEN** | İnsan onayına geldi → insan **reject** etti |

Gözlem: retry sırasında task package'a giren `quality_feedback`,
Maven hatasının satır/sütun bilgisini (`[11,22] ';' expected`) içeriyordu —
agent neyi düzelteceğini tahmin etmez, rapordan okur.

## Senaryo 1 devamı: İnsan müdahalesi + normale dönüş
- İnsan reject sonrası gate script'i `mvn clean test` ile sertleştirildi (ADR-0003)
- Yeni thread'de düzeltilmiş kod: attempt 1'de **gerçek YEŞİL** → merge

## Senaryo 2: Escalate (thread: live-demo-kronik-basarisiz-gorev)
| Deneme | Gate | Sistem tepkisi |
|--------|------|----------------|
| 1 | KIRMIZI | retry |
| 2 | KIRMIZI | retry |
| 3 | KIRMIZI | **escalated** — retry yok, onay yok, doğrudan insana devir |

Gözlem: `attempt >= max_attempts` koşulu conditional edge'de çalıştı;
sistem 3 başarısızlıktan sonra kendini durdurdu. Sonsuz döngü yok.

## Çıkan dersler
1. Gate'ler `clean build` çalıştırır (ADR-0003) — false green ölümcüldür.
2. İnsan onayı formalite değildir: bu drill'de ikinci savunma hattı olarak
   false green'i yakaladı.
3. Drill'ler (kaos testi) sistemin kendi zaaflarını buldurur —
   bu bulgu, planlı testle değil canlı tatbikatla çıktı.
