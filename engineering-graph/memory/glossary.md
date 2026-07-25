# Glossary — MeldeHub Terimler Sözlüğü

Agent'lar ve insanlar aynı dili konuşsun diye. Belirsizlik varsa bu dosya hakemdir.

## Süreç terimleri
- **Spec (şartname):** Bir görevin amacı + kapsamı + kabul kriterleri. Olmadan kod yazılmaz.
- **Gate (kapı):** Otomatik kontrol noktası. Geçemeyen iş bir sonraki aşamaya ilerleyemez. Gate karar vermez, ölçer.
- **Quality gate:** Test + derleme + mimari kontrollerden oluşan kapı. `scripts/run-quality-checks.sh` çalıştırır.
- **Security gate:** Secret taraması + bağımlılık açığı kontrolünden oluşan kapı. Kırmızıda retry yok, direkt escalate.
- **Human-in-the-loop (insan onayı):** Sistemin durup insandan karar beklediği zorunlu nokta.
- **Retry (yeniden deneme):** Kırmızı gate sonrası görevin, hata raporuyla (feedback) agent'a geri dönmesi.
- **Escalate (yükseltme):** Deneme sınırı dolunca veya güvenlik bulgusunda işin otomatik olarak insana devredilmesi.

## Graf / orkestrasyon terimleri
- **Node (düğüm):** Graftaki tek bir iş adımı (planner, implementer, gate...).
- **Edge (kenar):** İki düğüm arası koşulsuz geçiş: "A bitince hep B'ye git".
- **Conditional edge (koşullu kenar):** Gate sonucuna göre rota seçen geçiş: "yeşilse ileri, kırmızıysa geri".
- **State (durum):** Düğümler arasında akan paylaşılan veri paketi (`GraphState`). Grafiğin kısa süreli hafızası.
- **Interrupt:** LangGraph'te grafiğin kendini dondurması. İki türü var: **delegasyon interrupt'i** (uzman agent'ın kod yazmasını bekler) ve **onay interrupt'i** (insanın merge/reject kararını bekler).
- **Resume:** Donmuş grafiğe cevabı besleyip uyandırma işlemi (`Command(resume=...)`).
- **Checkpointer:** Graf durumunu kaydeden bileşen; duran grafın kaldığı yerden devamını sağlar.
- **MemorySaver:** Durumu RAM'de tutan checkpointer. Süreç kapanınca her şey uçar; tek süreçlik işlerde yeterli.
- **SqliteSaver:** Durumu diske (SQLite dosyası) yazan checkpointer. Süreçler arasında, hatta günler sonra bile devam sağlar.
- **thread_id:** Bir graf çalışmasının kimliği. Resume, doğru görevi bu kimlikle bulur.
- **Orchestrator (orkestra şefi):** Grafiği çalıştıran, görevleri agent'lara dağıtan bileşen.
- **Engineering Graph:** Geliştirme sürecinin graf olarak kodlanmış hali: görevler düğümler arasında akar.

## Canlı döngü terimleri
- **Task package (görev paketi):** Agent'a devredilen sözleşme dosyası: hedef, spec, hangi agent, kanıtın bırakılacağı yol; retry'da ayrıca `quality_feedback` (kırılan testin raporu).
- **Evidence (kanıt):** Agent'ın "ne ürettim, nasıl doğruladım, hafızaya etkisi ne, neyi dışarıda bıraktım" raporu. İnsan içindir; sistemin kararı exit code'a dayanır.
- **Exit code (çıkış kodu):** Bir programın bittiğinde döndürdüğü sayı: 0 = başarı, diğer her şey = hata. Gate'lerin ölçtüğü tek gerçek.
- **Delegasyon:** Grafiğin kod yazma işini uzman agent'a devretmesi; graf bu sırada donmuş bekler.
- **False green (yanıltıcı yeşil):** Kod bozukken gate'in YEŞİL dönmesi. En tehlikeli arıza türü: sistem "her şey yolunda" derken yolunda değildir. Faz 5'te yaşandı → ADR-0003.
- **Incremental compilation (artımlı derleme):** Sadece değişen dosyaları derleme optimizasyonu. Başarısız derlemelerden sonra "değişiklik yok" yanılgısına düşüp false green üretebilir (Maven'da yaşandı).
- **Clean build:** `target/` gibi tüm derleme çıktıları silinip sıfırdan derleme. Yavaştır ama tekrarlanabilir ve dürüsttür; gate'lerde zorunludur.

## Git / izolasyon terimleri
- **Worktree:** Aynı reponun ikinci bir fiziksel çalışma kopyası; kendi klasörü ve kendi dalı (branch) vardır. Agent izolasyonu için kullanılır — `scripts/worktree-new.sh` açar.
- **Branch (dal):** Aynı kod tabanında paralel bir zaman çizelgesi. Agent'ın denemeleri kendi dalında yaşar; main ancak merge ile etkilenir.
- **Merge:** Bir dalın işini başka bir dala (genelde main'e) katma işlemi. Bizde ancak gate'ler yeşil + insan onayı sonrası yapılır.
- **Fast-forward merge:** Main hiç ilerlemediyse merge'in "ok işaretini ileri alması" — birleştirme commit'i olmadan, temiz tarih.
- **İzolasyon:** Her işin kendi kum havuzunda (worktree + dal) yapılması; bir agent'ın dağınıklığı başkasını veya main'i kirletmez.

## Repository memory terimleri
- **ADR (Architecture Decision Record):** Bir mimari kararın belgesi: bağlam, karar, sonuçlar, alternatifler. Asla silinmez; geçersizse "superseded" olur.
- **Superseded (geçersiz kılındı):** Bir ADR'nin artık geçerli olmadığını ve yerini hangi yeni ADR'nin aldığını gösteren durum işareti.
- **Code-map:** Reponun haritası — ne nerede, kim kiminle konuşur. Gerçek yapıyla çelişirse derhal güncellenir.
- **Glossary:** Bu dosya — ortak dil sözlüğü.
- **Repository memory:** ADR + code-map + glossary üçlüsü; sistemin kalıcı hafızası.

## Backend terimleri
- **Maven:** Java'nın derleme + bağımlılık yöneticisi; `pom.xml`'i okur.
- **pom.xml:** Maven projesinin kimlik kartı: bağımlılıklar, Java sürümü, build ayarları.
- **Actuator:** Spring Boot'un üretim izleme eklentisi; `/actuator/health` endpoint'i uygulamanın nabzını dışarı verir (Kubernetes probe'ları buraya bağlanır).
- **Duman testi (smoke test):** "Uygulama ayağa kalkıyor mu?" sorusunu soran en temel test (Spring'de `contextLoads`). Adı elektronikten gelir: prize tak, duman çıkıyor mu bak.

## Domain terimleri (MeldeHub)
- **Meldung (ihbar):** Vatandaştan gelen bir bildirim (çukur, arıza, şikâyet).
- **Vaka (case):** İhbarın sisteme düşmüş, yönlendirilmiş hali.
- **SLA:** Bir vakanın çözülmesi gereken azami süre.
