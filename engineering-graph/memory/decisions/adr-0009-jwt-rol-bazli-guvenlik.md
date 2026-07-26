# ADR 0009 — JWT tabanlı rol bazlı güvenlik (stateless, refresh token'sız)

- **Durum (Status):** Kabul edildi
- **Tarih:** 2026-07-26

## Bağlam (Context)

CASE-201: MeldeHub endpoint'leri bugüne kadar tamamen anonim açıktı — herkes
ihbar verebilir, tüm vakaları okuyabilir ve durumlarını değiştirebilirdi.
Kamu gerçekçiliği gereği iki ayrı rol lazım: vatandaş (CITIZEN) ihbar verir,
belediye operatörü (OPERATOR) vakaları görür ve yönetir. Sorular: Kimlik
doğrulama nasıl yapılacak? Oturum durumu nerede tutulacak? Roller nasıl
modellenecek?

## Karar (Decision)

1. **JWT (HS256) + stateless güvenlik:** Login (`POST /api/auth/login`)
   başarıda 8 saat geçerli, imzalı bir Bearer token döner. Sunucuda oturum
   (session) tutulmaz; her istek `JwtAuthenticationFilter` ile token'dan
   doğrulanır. Kamu ölçeğinde yatay çoğaltılan (scale-out) backend'lerde
   paylaşılan session store (Redis vb.) işletme yükü ortadan kalkar —
   herhangi bir backend kopyası herhangi bir isteği bağımsız doğrular.
2. **Roller enum + DB'de:** Rol seti kapalı ve küçük (CITIZEN, OPERATOR) →
   `Role` enum'u DB'ye string yazılır (AppUser entity, V2 migration).
   Yeni rol eklemek bilinçli olarak kod değişikliği ister (rol modeli
   deploy ile gözden geçirilir), ama kullanıcı bazında rol ataması veri
   değişikliğidir, deploy gerektirmez.
3. **Şifreler BCrypt:** DB'de asla düz metin şifre yok; sadece
   BCryptPasswordEncoder hash'i. Seed kullanıcıların (`citizen`, `operator`)
   hash'leri V2 migration'a gömülüdür — ikisinin de şifresi `meldehub123`
   (demo; README'de belgelenmiştir).
4. **Refresh token YOK:** Bilinçli kapsam kararı. Demo/demo-ölçekli
   uygulamada 8 saatlik token bir iş gününü kapsar; süresi dolunca
   kullanıcı tekrar login olur. Refresh token eklemek token iptal
   listesi, rotasyon ve ek saldırı yüzeyi demektir — gerçek ihtiyaç
   doğana kadar borç olarak backlog'da tutulur.
5. **Secret env'den:** `JWT_SECRET` ortam değişkeni → `app.jwt.secret`.
   Koda gömülü secret yok; compose'da demo değeri, `application.yml`'de
   "production'da değiştir" yorumlu geliştirme varsayılanı var.
6. **Yetki kuralları URL + HTTP metodu bazında:** `POST /api/cases` →
   CITIZEN/OPERATOR; `GET` ve `PATCH /api/cases/**` → sadece OPERATOR;
   login, Swagger ve `/actuator/health` (Docker healthcheck) açık.
   401/403 cevapları `{"error": "..."}` biçiminde — mevcut
   GlobalExceptionHandler sözleşmesiyle aynı.

## Sonuçlar (Consequences)

- (+) Oturum durumu yok → backend stateless, yatay ölçekleme ve restart
  güvenli; logout/şifre değişikliği gibi senaryolar basit
- (+) Yetki kuralları SecurityConfig'te tek yerden okunur; 401/403 biçimi
  API genelinde tutarlı
- (+) Frontend token'ı çözümlemez; kullanıcı/rol bilgisini login
  cevabından alır → token şeması değişirse frontend etkilenmez
- (−) Token iptal edilemez (stateless'in doğası): bir token çalınırsa
  8 saat boyunca geçerlidir. Azaltım: kısa ömür (8s), HTTPS zorunluluğu
  (production notu), gerektiğinde JWT_SECRET rotasyonu tüm token'ları
  geçersiz kılar
- (−) localStorage XSS'e karşı httpOnly cookie'den zayıftır; SPA + aynı
  origin (ADR-0007) ve demo kapsamında kabul edilebilir risk

## Alternatifler

- **Session tabanlı (Spring Session + Redis):** reddedildi — kamu
  ölçeğinde yatay çoğaltmada ek altyapı ve işletme yükü; stateless JWT
  aynı işi sıfır ek bileşenle yapar
- **OAuth2 / Keycloak (harici IdP):** reddedildi — demo kapsamında fazla
  ağır; ileride kurumsal SSO gerekirse resource-server'a geçiş yolu açık
  (JWT sözleşmesi zaten standart)
- **Rolleri JWT'ye gömüp DB'de tutmamak:** reddedildi — rol değişikliği
  eski token'larda 8 saat geçerliliğini korurdu; DB tek gerçek kaynak
  kalır, token sadece login anındaki rolün imzalı kopyasıdır
