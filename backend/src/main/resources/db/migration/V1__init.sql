-- V1 — ilk şema: cases tablosu (Case entity'si, ADR-0005)
--
-- Kaynak doğruluk: backend/src/main/java/ch/meldehub/domain/Case.java
--   - id              : UUID, GenerationType.UUID          → uuid PK
--   - title           : String, nullable=false             → varchar(255)
--   - description     : String, nullable=false, length=2000 → varchar(2000)
--   - category/status : EnumType.STRING                    → varchar(255)
--   - location        : String, nullable=false             → varchar(255)
--   - reporterEmail   : String, nullable=false             → varchar(255)  (snake_case)
--   - createdAt/updatedAt : Instant, nullable=false        → timestamp(6) with time zone
--
-- Flyway şemayı kurar; Hibernate ddl-auto: validate ile sadece doğrular.

CREATE TABLE cases (
    id             uuid                        NOT NULL,
    title          varchar(255)                NOT NULL,
    description    varchar(2000)               NOT NULL,
    category       varchar(255)                NOT NULL,
    status         varchar(255)                NOT NULL,
    location       varchar(255)                NOT NULL,
    reporter_email varchar(255)                NOT NULL,
    created_at     timestamp(6) with time zone NOT NULL,
    updated_at     timestamp(6) with time zone NOT NULL,
    CONSTRAINT cases_pkey PRIMARY KEY (id)
);
