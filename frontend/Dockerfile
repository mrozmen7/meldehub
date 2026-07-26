# ---- Build aşaması ----
FROM node:24-alpine AS build
WORKDIR /app
# Lockfile ile deterministik kurulum; package değişmedikçe layer cache çalışır
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npm run build

# ---- Çalıştırma aşaması ----
FROM nginx:1.27-alpine
# SPA fallback + /api proxy (tek origin — ADR-0007)
COPY nginx.conf /etc/nginx/conf.d/default.conf
# Angular application builder çıktısı: dist/meldehub-frontend/browser
COPY --from=build /app/dist/meldehub-frontend/browser /usr/share/nginx/html
EXPOSE 80
