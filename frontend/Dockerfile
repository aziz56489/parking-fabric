# Stage 1: build the Angular app.
FROM node:20-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npx ng build --configuration production

# Stage 2: serve it with nginx, forwarding /api to the gateway.
FROM nginx:1.27-alpine
# Defaults for docker compose; on Railway set API_URL=http://gateway-service.railway.internal:8080 (PORT is set by Railway).
ENV PORT=80 \
    API_URL=http://gateway-service:8080
# Only these variables are substituted, so nginx's own $host etc. stay untouched.
ENV NGINX_ENVSUBST_FILTER="^(PORT|API_URL)$"
COPY nginx.conf.template /etc/nginx/templates/default.conf.template
COPY --from=build /app/dist/parking-front/browser /usr/share/nginx/html
EXPOSE 80
