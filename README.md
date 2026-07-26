# MeldeHub

Belediye vaka yönlendirme & entegrasyon platformu + onu inşa eden Engineering Graph sistemi.

Vatandaş ihbarları (web formu, legacy API, CSV import) REST API üzerinden sisteme düşer,
vaka yaratılınca Kafka'ya `case-created` event'i basılır ve routing consumer kategoriyi
doğru belediye birimine yönlendirir. Frontend (Angular) ve backend (Spring Boot) production'da
tek origin'den (nginx) servis edilir — ADR-0007.

## Kimlik doğrulama (CASE-201)

Tüm uçlar JWT ile korunur (ADR-0009): önce `POST /api/auth/login` ile token alınır,
sonra her istekte `Authorization: Bearer <token>` gönderilir. Arayüz bunu otomatik yapar
(login ekranı → token localStorage'da → interceptor her /api isteğine ekler).

Demo kullanıcıları (V2 migration ile seed edilir, ikisinin de şifresi aynı):

| Kullanıcı | Şifre | Rol | Yetki |
|---|---|---|---|
| `citizen` | `meldehub123` | CITIZEN | İhbar verme (`POST /api/cases`) |
| `operator` | `meldehub123` | OPERATOR | İhbar + vaka listesi/detay + durum yönetimi (`GET`, `PATCH`) |

Kurallar: login, Swagger ve `/actuator/health` açık; `POST /api/cases` → CITIZEN veya
OPERATOR; `GET`/`PATCH /api/cases/**` → sadece OPERATOR. Token 8 saat geçerlidir;
refresh token bilinçli olarak yoktur (süre bitince tekrar giriş yapılır).

## Production paketi ile çalıştırma (docker-compose)

Ön koşul: Docker. Host'ta yalnızca 4200 ve 8080 portları kullanılır; PostgreSQL ve
Kafka host'a açılmaz.

```bash
docker compose up --build
```

Servisler: `postgres` (16-alpine), `kafka` (tek node KRaft), `backend` (Spring Boot,
Flyway ilk açılışta şemayı kurar), `frontend` (Angular build + nginx).

### Portlar

| Adres | Açıklama |
|---|---|
| http://localhost:4200/ | Kullanıcı arayüzü (Angular, nginx) |
| http://localhost:4200/api/cases | REST API (nginx → backend proxy, tek origin) |
| http://localhost:8080/ | Backend'e doğrudan erişim (debug) |
| http://localhost:4200/swagger-ui.html | Swagger UI (nginx üzerinden) |

### Demo akışı

1. http://localhost:4200/ adresini aç — seni **giriş ekranı** karşılar.
   `citizen` / `meldehub123` ile giriş yap (ihbarcı olarak).
2. İhbar formunu doldur ve gönder (kategori ör. LIGHTING, konum, e-posta).
   Vaka `NEW` durumuyla kaydedilir; arka planda `case-created` event'i Kafka'ya
   basılır ve routing consumer kategoriyi birime eşler (`docker compose logs backend`).
3. Çıkış yap, bu kez `operator` / `meldehub123` ile giriş yap — operatör panelinde
   durumu ilerlet: NEW → TRIAGED → IN_PROGRESS → RESOLVED → CLOSED.
   Geçersiz geçişler kural gereği reddedilir. (Vatandaş rolü paneli göremez — 403.)

Kapatma: `docker compose down` (veriyi silmek için `docker compose down -v`).

## Testler ve quality gate'ler

```bash
cd backend && mvn clean verify        # tüm testler + JaCoCo %80 coverage gate
cd frontend && npm test -- --watch=false
./scripts/run-quality-checks.sh       # tüm gate'ler tek seferde
```

## Bu repo iki şeydir

1. **Ürün:** Heterojen kaynaklardan (web formu, legacy API, CSV import) gelen vatandaş
   ihbarlarını Kafka üzerinden normalize edip doğru departmana yönlendiren sistem.
2. **Süreç:** Bu ürünü inşa eden agentic mühendislik sistemi — uzman agent'lar,
   izole worktree'ler, otomatik quality/security gate'leri, insan onay noktaları
   ve repository memory (ADR tabanlı kalıcı hafıza).

## Yapı

Detay için: `engineering-graph/memory/code-map.md`
Kurallar için: `AGENTS.md`
Kararlar için: `engineering-graph/memory/decisions/`
