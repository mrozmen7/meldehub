-- V2 — kullanıcı tablosu + seed kullanıcılar (CASE-201, ADR-0009)
--
-- Kaynak doğruluk: backend/src/main/java/ch/meldehub/domain/AppUser.java
--   - id           : UUID, GenerationType.UUID          → uuid PK
--   - username     : String, unique, nullable=false     → varchar(255) + UNIQUE
--   - passwordHash : String, nullable=false             → varchar(255)  (snake_case)
--   - role         : EnumType.STRING (CITIZEN/OPERATOR) → varchar(255)
--
-- Şifreler BCrypt (strength 10) ile hash'li — düz metin şifre ASLA saklanmaz.
-- Her iki seed kullanıcının şifresi: meldehub123 (demo; production'da değiştirilir).
-- Hash'ler spring-security-crypto BCryptPasswordEncoder ile üretildi (elle yazılmadı).

CREATE TABLE users (
    id            uuid         NOT NULL,
    username      varchar(255) NOT NULL,
    password_hash varchar(255) NOT NULL,
    role          varchar(255) NOT NULL,
    CONSTRAINT users_pkey PRIMARY KEY (id),
    CONSTRAINT users_username_uk UNIQUE (username)
);

INSERT INTO users (id, username, password_hash, role) VALUES
    ('7c9e6679-7425-40de-944b-e07fc1f90ae1', 'citizen',
     '$2a$10$XT07kPwLIGJPWnORR3lhjuezA1xaEn9pamxCWNHTty4kabb5EpAIu', 'CITIZEN'),
    ('8d0f7780-8536-51ef-a55c-f180d2a01bf2', 'operator',
     '$2a$10$MLOw.no9uyjLzeHURvjlQujGQLiV/ljFJp5U1MQ33LI1.yQuA5O1W', 'OPERATOR');
