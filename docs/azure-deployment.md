# Azure production deployment

The production environment is defined by the subscription-scope `infra/main.bicep` orchestrator and resource-group modules under `infra/modules`.

## Resources

- Resource group `SREagent-lab`
- VNet-integrated Azure Container Apps environment with Store and API apps
- Basic Azure Container Registry with managed-identity image pull
- Private PostgreSQL Flexible Server and Private DNS
- Key Vault with RBAC, purge protection, and 90-day soft delete
- Log Analytics and workspace-based Application Insights
- Azure SRE Agent PaaS and its Azure Monitor Workspace in Australia East
- Read-only Monitoring Reader and Log Analytics Reader roles for the SRE Agent system-assigned identity

Operation Pulse is not deployed. Its former Container App, Entra app registration, and `operation-pulse` ACR repository were removed on 2026-08-13.

The application workload remains in Japan East. Azure SRE Agent is deployed in Australia East because `Microsoft.Monitor/observabilityAgents` is not available in Japan East. The agent uses Manual mode, so it investigates and proposes mitigations without applying changes automatically. Azure SRE Agent incurs always-on Azure Agent Unit charges for as long as the agent exists.

## Validate

```powershell
az bicep build --file infra/main.bicep
./infra/deploy.ps1 -ValidateOnly
./infra/deploy.ps1 -WhatIf
```

The first command is local-only. The second validates the deployment, and the third previews Azure changes. Neither script mode creates workload resources.

## Deploy

```powershell
./infra/deploy.ps1 -ResourceGroupName SREagent-lab -CostCenter demo
```

The script generates the PostgreSQL password in memory, stores secrets only in a temporary parameter file, and removes that file in `finally`. It deploys bootstrap apps, builds immutable images in ACR, and applies final app revisions.

The signed-in user must be allowed to create role assignments. For service-principal execution, pass its object ID with `-DeploymentPrincipalId`.

The deployment uses Azure Resource Manager incremental mode. Removing a resource declaration from Bicep does not delete an already deployed resource. When retiring a component, first apply the new configuration and verify retained workloads, then explicitly delete only the retired resources.

For the completed Operation Pulse retirement, the explicit cleanup covered:

- Container App `azpuldepgzxcukhrdm`
- Entra app registration with the exact display name `Operation Pulse - SREagent-lab`
- ACR repository `operation-pulse`

Use an exact match and inspect the result before deleting tenant-level Entra objects:

```powershell
$apps = az ad app list --display-name 'Operation Pulse - SREagent-lab' `
	--query "[?displayName=='Operation Pulse - SREagent-lab'].{id:id,appId:appId,displayName:displayName}" `
	--output json | ConvertFrom-Json

if (@($apps).Count -ne 1) {
		throw "Expected one exact Operation Pulse app registration; found $(@($apps).Count)."
}

az ad app delete --id $apps[0].id
```

The current IaC and deployment script do not recreate any of these Pulse resources.

## Operations

Liquibase applies versioned database changes when the API starts with the `production` profile. PostgreSQL is private; no permanent administration VM is deployed. Use Azure control-plane restore operations for recovery and an approved temporary VNet-connected job for exceptional SQL access.

The low-cost profile intentionally uses scale-to-zero, a single-zone Burstable database, seven-day local backups, and no WAF or paid DDoS plan. It targets manual recovery within four hours and at most one hour of data loss; upgrade HA and backup settings before treating the store as business-critical.

## Current production endpoints

| Component | Endpoint |
| --- | --- |
| Store | <https://azstodepgzxcukhrdm.livelyfield-29cce79e.japaneast.azurecontainerapps.io> |
| API | <https://azapidepgzxcukhrdm.livelyfield-29cce79e.japaneast.azurecontainerapps.io> |

The deployed Container Apps use the explicit `Consumption` workload profile with `minReplicas: 0`. Their application target ports are 8080 for Store and 8081 for API.

## Verification record

The production state was verified on 2026-08-13 after Operation Pulse removal.

| Check | Result |
| --- | --- |
| Bicep build and Azure validation | Succeeded |
| Final subscription deployment | Succeeded |
| Store request | HTTP 200 |
| `GET /api/wines` | HTTP 200, six products |
| Retired `GET /api/demo/incidents` | HTTP 404 |
| Container Apps | Store and API only |
| Azure SRE Agent | Retained as `azsredepgzxcukhrdm` in Australia East |
| Operation Pulse Entra registrations | 0 |
| `operation-pulse` ACR repository | Absent |

The backend Maven tests passed. A local frontend build could not be completed because npm dependency installation did not receive a registry response; the frontend image did build successfully in ACR, was deployed, and passed the production HTTP check.
