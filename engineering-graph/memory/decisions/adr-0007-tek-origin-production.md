# ADR 0007 — Production'da tek origin: nginx reverse proxy, CORS yok

- **Durum (Status):** Kabul edildi
- **Tarih:** 2026-07-26

## Bağlam (Context)

CASE-187: MeldeHub production dağıtım paketi (docker-compose).
Sorular: Frontend (Angular) ve backend (Spring Boot) farklı konteynerlerde
çalışırken tarayıcı istekleri nasıl akacak? CORS açmak gerekiyor mu?

## Karar (Decision)

1. **Tek origin:** Frontend, nginx üzerinden servis edilir (`:4200` →
   konteynerde `:80`). API istekleri aynı origin'e, `/api/...` yoluna gider.
2. **nginx reverse proxy:** `location /api/` bloğu istekleri `backend:8080`'e
   proxy'ler (compose servis adı üzerinden, container içi DNS).
   `/v3/api-docs` ve `/swagger-ui` de aynı şekilde proxy'lenir →
   dokümantasyon da tek origin'den erişilebilir.
3. **CORS konfigürasyonu YOK:** Tarayıcı için her şey aynı origin'dir;
   backend'de `CrossOrigin`/CORS filtresi açılmaz. Saldırı yüzeyi büyümez.
4. **Dev ile aynı mental model:** Geliştirme ortamındaki `proxy.conf.json`
   (Faz 10, `/api` → `localhost:8080`) production'daki nginx proxy'nin
   birebir karşılığıdır. Kod dev/prod arasında farklı davranmaz.

## Sonuçlar (Consequences)

- (+) CORS konfigürasyon borcu yok; preflight trafiği yok; cookie/oturum
  gelecekte eklenirse same-origin varsayılanı çalışır
- (+) Dev ve prod aynı istek şeklini kullanır → "bende çalışıyordu" riski azalır
- (−) Tüm trafik nginx'ten geçer → nginx tek giriş noktası (bilinçli:
  zaten tek sayfalık demo uygulaması; ölçekleme gerekirse LB önüne konur)
- (−) Backend `:8080` host'a açık kalır (demo/debug kolaylığı); sertleştirme
  istersek bu port kapatılıp sadece nginx üzerinden erişilir

## Alternatifler

- Backend'de CORS açmak (`@CrossOrigin` / `WebMvcConfigurer`) → reddedildi:
  gereksiz konfigürasyon, genişletilmesi kolay hata yapılır, dev modeliyle
  tutarsız
- Frontend'i backend'den (Spring static resources) servis etmek → reddedildi:
  iki release döngüsünü kenetler; nginx ayrı ölçeklenir/önbellekler
