FROM cgr.dev/chainguard/node:latest-dev AS build

USER root
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci --ignore-scripts
COPY . .

ARG VITE_API_BASE_URL=http://localhost:8081
ARG VITE_APPLICATIONINSIGHTS_CONNECTION_STRING
ENV VITE_API_BASE_URL=${VITE_API_BASE_URL}
ENV VITE_APPLICATIONINSIGHTS_CONNECTION_STRING=${VITE_APPLICATIONINSIGHTS_CONNECTION_STRING}
RUN npm run build

FROM cgr.dev/chainguard/nginx:latest

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 8080