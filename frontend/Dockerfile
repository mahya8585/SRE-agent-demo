FROM cgr.dev/chainguard/node:latest-dev AS build

USER root
WORKDIR /app
COPY package.json .npmrc ./
RUN npm install --ignore-scripts
COPY . .

ARG SITE=ops
ARG VITE_API_BASE_URL
ENV VITE_API_BASE_URL=${VITE_API_BASE_URL}
RUN test -n "$VITE_API_BASE_URL" || (echo 'VITE_API_BASE_URL build argument is required.' >&2; exit 1)
RUN npm run build:${SITE} && cp -R dist/${SITE} /site

FROM cgr.dev/chainguard/nginx:latest

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /site /usr/share/nginx/html

EXPOSE 8080