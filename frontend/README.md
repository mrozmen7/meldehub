# MeldeHub — Frontend

İsviçre belediyeleri için vatandaş ihbar platformunun Angular frontend'i.
Backend: Spring Boot — **http://localhost:8080** üzerinde çalışıyor olmalı.

## Çalıştırma

```bash
npm install
npm start          # http://localhost:4200 — /api istekleri proxy ile 8080'e gider
```

> Not: `npm start` öncesi backend'in 8080 portunda ayakta olduğundan emin olun
> (proxy.conf.json: `/api` → `http://localhost:8080`).

## Diğer komutlar

```bash
npm run build      # production build (dist/)
npm test           # unit testler (Vitest, headless)
```

## Sayfalar

- `/report` — vatandaş ihbar formu (varsayılan sayfa)
- `/operator` — operatör paneli (vaka listesi + durum yönetimi)
