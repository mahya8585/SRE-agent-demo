# Azure Deployment Plan

> **Status:** Ready for Validation

Generated: 2026-08-26

## 1. Project Overview

**Goal:** Redeploy the existing Maison Vigne Store, Admin, and API Azure Container Apps with the corrected frontend API URL build configuration.

**Path:** Modify existing deployment

## 2. Requirements

| Attribute | Value |
| --- | --- |
| Classification | Demo / Development |
| Scale | Small |
| Budget | Cost-Optimized |
| Subscription | MCAPS-Hybrid-REQ-73654-2024-maishid (`70bcc220-4d88-48f2-a59a-77bae4785eac`) |
| Primary location | Japan East (`japaneast`) |
| SRE Agent location | Australia East (`australiaeast`) |
| Resource group | `SREagent-lab` |

The user confirmed this subscription and location on 2026-08-26.

## 3. Components Detected

| Component | Type | Technology | Path |
| --- | --- | --- | --- |
| Store | Frontend | Vue / Vite / Nginx | `frontend/` |
| Admin | Frontend | Vue / Vite / Nginx | `admin-frontend/` |
| API | API | Java / Spring Boot | `backend/` |
| Infrastructure | IaC | Bicep / Azure CLI / PowerShell | `infra/` |

## 4. Recipe Selection

**Selected:** Azure CLI + Bicep

**Rationale:** The repository already uses a tested two-phase bootstrap/application deployment in `infra/deploy.ps1`. Converting this deployment to azd is outside the requested redeployment.

## 5. Architecture

Store, Admin, and API run as Azure Container Apps in one managed environment. Images are built in ACR. PostgreSQL, Key Vault, Blob Storage, managed identities, private endpoints, Log Analytics, Application Insights, and Azure SRE Agent remain unchanged.

| Component | Existing Azure service | Update |
| --- | --- | --- |
| Store | Container App `azstodepgzxcukhrdm` | New image revision |
| Admin | Container App `azadmdepgzxcukhrdm` | New image revision with production API URL |
| API | Container App `azapidepgzxcukhrdm` | New image revision |
| ACR | `azcrdepgzxcukhrdm` | Add three immutable image tags |
| PostgreSQL / Key Vault | Existing resources | Synchronized generated administrator password update by the established script |

## 6. Provisioning Limit Checklist

| Resource Type | Number to Deploy | Total After Deployment | Limit/Quota | Notes |
| --- | ---: | ---: | ---: | --- |
| Microsoft.App/managedEnvironments | 0 new | 1 | 50 | `az quota list`, `ManagedEnvironmentCount`, Japan East |
| Microsoft.App/containerApps | 0 new | 3 | Existing update | All three apps are in `Succeeded` state |

**Status:** All required resources exist and the deployment is within limits.

## 7. Execution Checklist

- [x] Analyze workspace and existing deployment
- [x] Confirm subscription and locations with user
- [x] Validate Container Apps quota
- [x] Confirm ACR `AcrPull` assignment for the pull identity
- [x] Confirm Key Vault Secrets User and Storage Blob Data Contributor for the API identity
- [x] Run local frontend builds and backend tests
- [x] Mark plan Ready for Validation
- [ ] All validation checks pass
	- [x] PowerShell syntax validation
	- [x] Admin frontend build
	- [x] Store frontend build
	- [x] Backend Maven tests
	- [ ] Bicep compilation
	- [ ] Azure subscription deployment validation
	- [ ] Azure subscription deployment What-If review
	- [x] Existing Container Apps, ACR, PostgreSQL, quota, and RBAC checks
- [ ] Execute deployment
- [ ] Verify endpoints and telemetry

## 8. Change Set

- Rebuild Store and Admin images with an explicit production `VITE_API_BASE_URL`.
- Reject missing frontend API build arguments.
- Reject non-HTTPS API URLs before ACR builds.
- Deploy Store, Admin, and API images under one new immutable image tag.

## 9. Deployment And Rollback

Run `infra/deploy.ps1` only after Bicep compilation, Azure validation, and What-If pass. The deployment uses Container Apps single-revision mode. If post-deployment checks fail, identify and reactivate the exact last known-good revision or redeploy the prior image tag `20260817164541` after confirming database compatibility.

## 10. Security

- Do not print or persist Application Insights connection strings, generated PostgreSQL passwords, tokens, or cookies.
- Keep managed identity, private networking, Key Vault, and ACR pull configuration unchanged.
- The deployment rotates the PostgreSQL administrator password and updates the corresponding Key Vault secret in the same deployment.

## 11. Validation Proof

Pending formal Azure validation.
