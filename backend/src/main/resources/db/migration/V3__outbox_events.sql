-- V3 — outbox tablosu: Transactional Outbox (CASE-252, ADR-0008 güncellemesi)
--
-- Kaynak doğruluk: backend/src/main/java/ch/meldehub/events/OutboxEvent.java
--   - id           : UUID, GenerationType.UUID      → uuid PK
--   - aggregateId  : UUID, nullable=false           → uuid  (snake_case)
--   - eventType    : String, length=100, nullable=false → varchar(100)
--   - payload      : String, columnDefinition=text  → text
--                    (jsonb DEĞİL: H2 test uyumu + ddl-auto:validate güvenliği;
--                    içerik zaten Jackson ile serialize edilmiş JSON string)
--   - createdAt    : Instant, nullable=false        → timestamp(6) with time zone
--   - published    : boolean, nullable=false        → boolean DEFAULT FALSE
--   - publishedAt  : Instant, nullable              → timestamp(6) with time zone
--
-- Flyway şemayı kurar; Hibernate ddl-auto: validate ile sadece doğrular.

CREATE TABLE outbox_events (
    id           uuid                        NOT NULL,
    aggregate_id uuid                        NOT NULL,
    event_type   varchar(100)                NOT NULL,
    payload      text                        NOT NULL,
    created_at   timestamp(6) with time zone NOT NULL,
    published    boolean                     NOT NULL DEFAULT FALSE,
    published_at timestamp(6) with time zone,
    CONSTRAINT outbox_events_pkey PRIMARY KEY (id)
);

-- Relay sorgusu hep "published = FALSE, created_at ASC" çalışır;
-- partial index sadece bekleyen satırları tutar (tablo büyüse bile tarama ucuz).
CREATE INDEX idx_outbox_unpublished ON outbox_events (created_at) WHERE published = FALSE;
