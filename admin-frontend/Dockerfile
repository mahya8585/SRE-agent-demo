FROM cgr.dev/chainguard/node:latest-dev AS build

USER root
WORKDIR /app
COPY package.json .npmrc ./
RUN npm install --ignore-scripts
COPY . .

ARG VITE_API_BASE_URL
ARG VITE_APPLICATIONINSIGHTS_CONNECTION_STRING_BASE64
ENV VITE_API_BASE_URL=${VITE_API_BASE_URL}
RUN test -n "$VITE_API_BASE_URL" || (echo 'VITE_API_BASE_URL build argument is required.' >&2; exit 1)
RUN export VITE_APPLICATIONINSIGHTS_CONNECTION_STRING="$(printf '%s' "$VITE_APPLICATIONINSIGHTS_CONNECTION_STRING_BASE64" | base64 -d)" && npm run build

FROM cgr.dev/chainguard/nginx:latest

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 8080