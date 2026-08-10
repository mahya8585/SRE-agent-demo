# SRE Agent Wine Demo

This workspace contains two independently deployable Vue sites backed by one Spring Boot API.

## Sites

| Site | Local URL | API surface | Purpose |
| --- | --- | --- | --- |
| Maison Vigne Operations | <http://localhost:3000> | `GET /api/wines` | Product and inventory operations |
| Operation Pulse | <http://localhost:3001> | `GET /api/demo/incidents`, `POST /api/demo/scenarios/{scenario}` | SRE monitoring and demo scenario controls |

The sites share frontend dependencies and base styles, but Vite produces separate artifacts. The operations bundle does not contain scenario controls, and the Pulse bundle does not contain inventory UI.

## Components

- Backend: Spring Boot 2.7 / Java 8 / Maven
- Frontend: Vue 3 + Vite
- Data: in-memory H2 for local development
- Infrastructure: Azure Container Apps, Azure Container Registry, managed identity, and Log Analytics

## Run locally

Start the API:

```powershell
Set-Location backend
mvn spring-boot:run
```

In separate terminals, start both sites:

```powershell
Set-Location frontend
npm install
npm run dev:ops
```

```powershell
Set-Location frontend
npm run dev:pulse
```

The default API endpoint is `http://localhost:8081`. To use another endpoint, copy `frontend/.env.example` to an appropriate Vite mode file such as `.env.ops.local` and set `VITE_API_BASE_URL`.

The backend accepts requests from both local frontend origins by default. Override the comma-separated list for deployed environments:

```powershell
$env:CORS_ALLOWED_ORIGINS = 'https://ops.example.com,https://pulse.example.com'
mvn spring-boot:run
```

## Build

Build the two frontend artifacts independently:

```powershell
Set-Location frontend
npm run build:ops
npm run build:pulse
```

Outputs are written to `frontend/dist/ops` and `frontend/dist/pulse`.

Build the container images after the target API URL is known:

```powershell
docker build frontend --build-arg SITE=ops --build-arg VITE_API_BASE_URL=https://API_HOST -t maison-vigne-ops:local
docker build frontend --build-arg SITE=pulse --build-arg VITE_API_BASE_URL=https://API_HOST -t operation-pulse:local
docker build backend -t wine-api:local
```

## Azure infrastructure

`infra/main.bicep` defines the following resources in Japan East:

- One Azure Container Registry with admin credentials disabled
- One user-assigned managed identity and an `AcrPull` role assignment
- One Log Analytics workspace and Container Apps environment
- Separate operations, Pulse, and API Container Apps with independent public URLs

The default parameters deploy Microsoft's Container Apps bootstrap image on port 80. This allows the registry and final host names to exist before the frontend images are built. For the application deployment, update `infra/main.parameters.json` with the three ACR image names and ports `8080`, `8080`, and `8081`. Build both frontend images with the bootstrap deployment's `apiUrl` output as `VITE_API_BASE_URL`.

Validate the template before deployment:

```powershell
az bicep build --file infra/main.bicep
az deployment group validate --resource-group RESOURCE_GROUP --template-file infra/main.bicep --parameters infra/main.parameters.json
az deployment group what-if --resource-group RESOURCE_GROUP --template-file infra/main.bicep --parameters infra/main.parameters.json
```

Authentication and authorization for the scenario injection API are intentionally outside this demo scope. CORS restricts browser origins but is not access control; add server-side authorization before exposing Operation Pulse beyond a controlled demo environment.
