# Code Map — MeldeHub

Bu dosya, reponun "haritası"dır. Her agent göreve başlamadan önce buraya bakarak
neyin nerede olduğunu öğrenir. Yapı değiştikçe bu dosya GÜNCELLENİR.

| Yol | Sahibi | Ne yapar? | Kiminle konuşur? |
|-----|--------|-----------|------------------|
| `.claude/agents/` | İnsan | Uzman agent tanımları | — |
| `engineering-graph/` | Orchestrator | Görev akışı, gate'ler, onay noktaları, hafıza | Tüm agent'lar |
| `engineering-graph/memory/` | Herkes | ADR'ler, code-map, glossary — sistemin hafızası | Her agent okur/yazar |
| `backend/` | spring-architect, kafka-expert | Spring Boot + Kafka backend (Faz 8-9'da dolacak) | PostgreSQL, Kafka |
| `frontend/` | angular-expert | Angular panel (Faz 10'da dolacak) | Backend API |
| `infra/` | İnsan + agent | Docker Compose: Kafka, PostgreSQL, Prometheus | Tüm servisler |
| `scripts/` | İnsan | worktree ve gate yardımcı script'leri | CI |
| `docs/` | Herkes | Eğitim notları, diyagramlar | — |

> Kural: Bu tablo ile gerçek yapı çelişirse, gerçek yapı haklıdır — tablo derhal güncellenir.
