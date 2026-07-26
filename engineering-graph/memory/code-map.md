# Code Map — MeldeHub

Bu dosya, reponun "haritası"dır. Her agent göreve başlamadan önce buraya bakarak
neyin nerede olduğunu öğrenir. Yapı değiştikçe bu dosya GÜNCELLENİR.

| Yol | Sahibi | Ne yapar? | Kiminle konuşur? |
|-----|--------|-----------|------------------|
| `.claude/agents/` | İnsan | Uzman agent tanımları (5 uzman) | — |
| `engineering-graph/orchestrator.py` | Orchestrator | Grafiği kurar ve çalıştırır (sim + live/resume CLI) | Tüm düğümler |
| `engineering-graph/state.py` | Orchestrator | GraphState: düğümler arası paylaşılan durum | Tüm düğümler |
| `engineering-graph/nodes/` | Orchestrator | planner, implementer, reviewer düğümleri | state |
| `engineering-graph/gates/` | Orchestrator | quality + security gate'leri (live modda script çalıştırır) | `scripts/` |
| `engineering-graph/checkpoints/` | Sistem | graph.db + task-package.json + evidence.md (runtime, gitignore'lu) | — |
| `engineering-graph/memory/` | Herkes | ADR'ler, code-map, glossary, evidence şablonu | Her agent okur/yazar |
| `backend/` | spring-architect, kafka-expert | Case domain'i ayakta (entity, repository, servis, REST API, yaşam döngüsü); Kafka Faz 9'da | PostgreSQL, Kafka |
| `frontend/` | angular-expert | Angular panel (Faz 10'da dolacak) | Backend API |
| `infra/` | İnsan + agent | Docker Compose: Kafka, PostgreSQL, Prometheus | Tüm servisler |
| `scripts/` | İnsan | run-quality-checks.sh, security-scan.sh, worktree-new.sh | Gate'ler + CI |
| `docs/` | Herkes | Eğitim notları, diyagramlar | — |

> Kural: Bu tablo ile gerçek yapı çelişirse, gerçek yapı haklıdır — tablo derhal güncellenir.
