# SRE Agent Wine Demo

This workspace contains a customer-facing Vue storefront backed by a Spring Boot API. Production orders and inventory are persisted in Azure Database for PostgreSQL, and Azure SRE Agent provides read-only operational investigation.

## Documentation

The complete system specification, including architecture, customer flows, API contracts, data retention, local operation, Azure infrastructure, validation results, and production limitations, is available in [docs/system-overview.md](docs/system-overview.md).

## Site

| Site | Local URL | Production URL | API surface | Purpose |
| --- | --- | --- | --- | --- |
| Maison Vigne Store | <http://localhost:3000> | <https://azstodepgzxcukhrdm.livelyfield-29cce79e.japaneast.azurecontainerapps.io> | `GET /api/wines`, `POST /api/orders` | Customer storefront, cart, and checkout |

The existing `ops` script and artifact name are retained for deployment compatibility.
Operation Pulse and its demo incident API have been retired and are not part of the application or Azure deployment.

## Components

- Backend: Spring Boot 2.7 / Java 8 / Maven
- Frontend: Vue 3 + Vite
- Data: in-memory H2 for local development; PostgreSQL 17 in Azure
- Infrastructure: Azure Container Apps, Azure Container Registry, PostgreSQL, managed identity, Azure Monitor, and Azure SRE Agent

## Run locally

Start the API:

```powershell
Set-Location backend
mvn spring-boot:run
```

In a separate terminal, start the site:

```powershell
Set-Location frontend
npm install
npm run dev:ops
```

The default API endpoint is `http://localhost:8081`. To use another endpoint, copy `frontend/.env.example` to an appropriate Vite mode file such as `.env.ops.local` and set `VITE_API_BASE_URL`.

The backend accepts requests from the local storefront origin by default. Override it for deployed environments:

```powershell
$env:CORS_ALLOWED_ORIGINS = 'https://store.example.com'
mvn spring-boot:run
```

## Build

Build the storefront:

```powershell
Set-Location frontend
npm run build:ops
```

The output is written to `frontend/dist/ops`.

Build the container images after the target API URL is known:

```powershell
docker build frontend --build-arg SITE=ops --build-arg VITE_API_BASE_URL=https://API_HOST -t maison-vigne-ops:local
docker build backend -t wine-api:local
```

## Azure infrastructure

`infra/main.bicep` is a subscription-scope orchestrator for the production environment in Japan East. Resource-group modules define:

- VNet-integrated Container Apps for Store and API
- Private PostgreSQL Flexible Server with versioned Liquibase migrations
- Key Vault and separate managed identities for image pull and API secret access
- Basic ACR, Log Analytics, and workspace-based Application Insights
- Azure SRE Agent PaaS with a dedicated Azure Monitor Workspace and read-only workload access

The deployment script creates bootstrap endpoints, builds immutable images in ACR, and applies the final revisions. Secrets are generated at runtime and are not stored in the committed parameter example.

Validate the template before deployment:

```powershell
az bicep build --file infra/main.bicep
./infra/deploy.ps1 -ValidateOnly
```

See [docs/azure-deployment.md](docs/azure-deployment.md) for deployment prerequisites, operations, and accepted low-cost availability tradeoffs.

## Current deployment

The production deployment in resource group `SREagent-lab` was verified on 2026-08-13:

- Store returned HTTP 200.
- `GET /api/wines` returned HTTP 200 with six products.
- The retired `GET /api/demo/incidents` endpoint returned HTTP 404.
- The resource group contained only the Store and API Container Apps.
- Azure SRE Agent `azsredepgzxcukhrdm` remained deployed in Australia East.
- The Operation Pulse Container App, Entra app registration, and ACR repository were removed.
