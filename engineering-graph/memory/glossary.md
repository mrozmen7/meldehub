# Glossary — MeldeHub Terimler Sözlüğü

Agent'lar ve insanlar aynı dili konuşsun diye. Belirsizlik varsa bu dosya hakemdir.

## Süreç terimleri
- **Spec (şartname):** Bir görevin amacı + kapsamı + kabul kriterleri. Olmadan kod yazılmaz.
- **Gate (kapı):** Otomatik kontrol noktası. Geçemeyen iş bir sonraki aşamaya ilerleyemez.
- **Quality gate:** Test + derleme + mimari kontrollerden oluşan kapı.
- **Security gate:** Secret taraması + bağımlılık açığı kontrolünden oluşan kapı.
- **Human-in-the-loop (insan onayı):** Sistemin durup insandan karar beklediği zorunlu nokta.
- **Interrupt:** LangGraph'te grafiğin insan onayı için duraklatılması.

## Mimari terimler
- **ADR (Architecture Decision Record):** Bir mimari kararın belgesi: bağlam, karar, sonuçlar, alternatifler.
- **Code-map:** Reponun haritası — ne nerede, kim kiminle konuşur.
- **Glossary:** Bu dosya — ortak dil sözlüğü.
- **Repository memory:** ADR + code-map + glossary üçlüsü; sistemin kalıcı hafızası.
- **Engineering Graph:** Geliştirme sürecinin graf olarak kodlanmış hali: görevler düğümler arasında akar.
- **Orchestrator (orkestra şefi):** Grafiği çalıştıran, görevleri agent'lara dağıtan bileşen.
- **Worktree:** Aynı reponun ikinci bir fiziksel çalışma kopyası; agent izolasyonu için kullanılır.

## Domain terimleri (MeldeHub)
- **Meldung (ihbar):** Vatandaştan gelen bir bildirim (çukur, arıza, şikâyet).
- **Vaka (case):** İhbarın sisteme düşmüş, yönlendirilmiş hali.
- **SLA:** Bir vakanın çözülmesi gereken azami süre.
