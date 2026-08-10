FROM cgr.dev/chainguard/node:latest-dev AS build

USER root
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .

ARG SITE=ops
ARG VITE_API_BASE_URL=http://localhost:8081
ENV VITE_API_BASE_URL=${VITE_API_BASE_URL}
RUN npm run build:${SITE} && cp -R dist/${SITE} /site

FROM cgr.dev/chainguard/nginx:latest

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /site /usr/share/nginx/html

EXPOSE 8080