[CmdletBinding()]
param(
    [string]$Location = 'japaneast',
    [string]$ResourceGroupName = 'SREagent-lab',
    [string]$SreAgentLocation = 'australiaeast',
    [string]$EnvironmentName = 'prod',
    [string]$CostCenter = 'demo',
    [string]$ImageTag = (Get-Date -Format 'yyyyMMddHHmmss'),
    [string]$DeploymentPrincipalId,
    [switch]$ValidateOnly,
    [switch]$WhatIf
)

$ErrorActionPreference = 'Stop'
$env:PYTHONUTF8 = '1'
[Console]::OutputEncoding = [System.Text.UTF8Encoding]::new($false)
$root = Split-Path -Parent $PSScriptRoot
$template = Join-Path $PSScriptRoot 'main.bicep'
$deploymentName = "maison-vigne-$ImageTag"

if (-not (Get-Command az -ErrorAction SilentlyContinue)) { throw 'Azure CLI is required.' }
az account show --only-show-errors | Out-Null
if ($LASTEXITCODE -ne 0) { throw 'Run az login before deployment.' }
if (-not $DeploymentPrincipalId) {
    $DeploymentPrincipalId = az ad signed-in-user show --query id -o tsv --only-show-errors
}
if (-not $DeploymentPrincipalId) {
    throw 'Specify -DeploymentPrincipalId when Azure CLI is authenticated as a service principal.'
}

$passwordBytes = New-Object byte[] 36
[Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($passwordBytes)
$postgresPassword = [Convert]::ToBase64String($passwordBytes) + 'aA1!'
$postgresLogin = 'wineadmin'
$temporaryParameters = Join-Path ([IO.Path]::GetTempPath()) "maison-vigne-$([guid]::NewGuid()).json"

function Write-Parameters([string]$Tag) {
    $parameters = @{
        '$schema' = 'https://schema.management.azure.com/schemas/2019-04-01/deploymentParameters.json#'
        contentVersion = '1.0.0.0'
        parameters = @{
            location = @{ value = $Location }
            resourceGroupName = @{ value = $ResourceGroupName }
            sreAgentLocation = @{ value = $SreAgentLocation }
            environmentName = @{ value = $EnvironmentName }
            deploymentPrincipalId = @{ value = $DeploymentPrincipalId }
            postgresAdministratorLogin = @{ value = $postgresLogin }
            postgresAdministratorPassword = @{ value = $postgresPassword }
            imageTag = @{ value = $Tag }
            costCenter = @{ value = $CostCenter }
        }
    }
    $parameters | ConvertTo-Json -Depth 8 | Set-Content -Path $temporaryParameters -Encoding UTF8
}

function Invoke-AcrBuild([string]$RegistryName, [string[]]$BuildArguments, [string]$FailureMessage) {
    $runId = & az acr build --registry $RegistryName @BuildArguments --no-logs --query runId -o tsv --only-show-errors
    if ($LASTEXITCODE -ne 0 -or -not $runId) { throw $FailureMessage }

    $runResourceId = az acr task show-run --registry $RegistryName --run-id $runId --query id -o tsv --only-show-errors
    if ($LASTEXITCODE -ne 0 -or -not $runResourceId) { throw "$FailureMessage Run lookup failed." }
    az resource wait --ids $runResourceId --api-version 2019-06-01-preview --custom "properties.status!='Queued' && properties.status!='Running'" --interval 5 --timeout 1800
    if ($LASTEXITCODE -ne 0) { throw "$FailureMessage Run wait failed. Run ID: $runId" }

    $runStatus = az acr task show-run --registry $RegistryName --run-id $runId --query status -o tsv --only-show-errors
    if ($LASTEXITCODE -ne 0 -or $runStatus -ne 'Succeeded') { throw "$FailureMessage Run ID: $runId; status: $runStatus" }
}

function Assert-HttpsUrl([string]$Value, [string]$Name) {
    $uri = $null
    if (-not [Uri]::TryCreate($Value, [UriKind]::Absolute, [ref]$uri) -or $uri.Scheme -ne 'https' -or -not $uri.Host) {
        throw "$Name must be an absolute HTTPS URL."
    }
}

function Get-SingleResource([object[]]$Resources, [scriptblock]$Filter, [string]$Name) {
    $matches = @($Resources | Where-Object $Filter)
    if ($matches.Count -ne 1) { throw "Expected exactly one $Name resource, found $($matches.Count)." }
    return $matches[0]
}

try {
    if ($ValidateOnly) {
        Write-Parameters -Tag 'bootstrap'
        az deployment sub validate --name $deploymentName --location $Location --template-file $template --parameters "@$temporaryParameters" --only-show-errors | Out-Null
        if ($LASTEXITCODE -ne 0) { throw 'Azure subscription deployment validation failed.' }
        Write-Output 'Azure validation succeeded. No resources were created.'
        return
    }
    if ($WhatIf) {
        Write-Parameters -Tag $ImageTag
        az deployment sub what-if --name $deploymentName --location $Location --template-file $template --parameters "@$temporaryParameters" --only-show-errors
        if ($LASTEXITCODE -ne 0) { throw 'Azure subscription deployment what-if failed.' }
        return
    }

    $resourceGroupExists = $null -ne (az group exists --name $ResourceGroupName --only-show-errors | Where-Object { $_ -eq 'true' })
    $existingApps = @()
    if ($resourceGroupExists) {
        $existingApps = @(az containerapp list --resource-group $ResourceGroupName --output json --only-show-errors | ConvertFrom-Json)
        if ($LASTEXITCODE -ne 0) { throw 'Existing Container Apps lookup failed.' }
    }

    if ($existingApps.Count -gt 0) {
        $storeApp = Get-SingleResource -Resources $existingApps -Filter { $_.properties.template.containers[0].name -eq 'store' } -Name 'Store Container App'
        $adminApp = Get-SingleResource -Resources $existingApps -Filter { $_.properties.template.containers[0].name -eq 'admin' } -Name 'Admin Container App'
        $apiApp = Get-SingleResource -Resources $existingApps -Filter { $_.properties.template.containers[0].name -eq 'api' } -Name 'API Container App'
        $registry = Get-SingleResource -Resources @(az acr list --resource-group $ResourceGroupName --output json --only-show-errors | ConvertFrom-Json) -Filter { $_.tags.application -eq 'maison-vigne' } -Name 'Azure Container Registry'
        if ($LASTEXITCODE -ne 0) { throw 'Existing registry lookup failed.' }
        $applicationInsights = Get-SingleResource -Resources @(az resource list --resource-group $ResourceGroupName --resource-type 'Microsoft.Insights/components' --output json --only-show-errors | ConvertFrom-Json) -Filter { $_.tags.application -eq 'maison-vigne' } -Name 'Application Insights'
        if ($LASTEXITCODE -ne 0) { throw 'Existing Application Insights lookup failed.' }
        $resourceGroupName = $ResourceGroupName
        $registryName = $registry.name
        $apiUrl = "https://$($apiApp.properties.configuration.ingress.fqdn)"
        $applicationInsightsName = $applicationInsights.name
    }
    else {
        Write-Parameters -Tag 'bootstrap'
        az deployment sub create --name "$deploymentName-bootstrap" --location $Location --template-file $template --parameters "@$temporaryParameters" --no-wait --only-show-errors --output none
        if ($LASTEXITCODE -ne 0) { throw 'Bootstrap deployment failed.' }
        az deployment sub wait --name "$deploymentName-bootstrap" --created --only-show-errors
        if ($LASTEXITCODE -ne 0) { throw 'Bootstrap deployment wait failed.' }
        $bootstrap = az deployment sub show --name "$deploymentName-bootstrap" --query properties.outputs -o json --only-show-errors | ConvertFrom-Json
        if ($LASTEXITCODE -ne 0) { throw 'Bootstrap deployment output retrieval failed.' }
        $resourceGroupName = $bootstrap.resourceGroupName.value
        $registryName = $bootstrap.registryName.value
        $apiUrl = $bootstrap.apiUrl.value
        $applicationInsightsName = $bootstrap.applicationInsightsName.value
    }
    Assert-HttpsUrl -Value $apiUrl -Name 'API URL'
    $applicationInsightsConnectionString = az monitor app-insights component show --resource-group $resourceGroupName --app $applicationInsightsName --query connectionString -o tsv --only-show-errors
    if ($LASTEXITCODE -ne 0 -or -not $applicationInsightsConnectionString) { throw 'Application Insights connection string lookup failed.' }
    $applicationInsightsConnectionStringBase64 = [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes($applicationInsightsConnectionString))

    Invoke-AcrBuild -RegistryName $registryName -BuildArguments @('--image', "wine-api:$ImageTag", '--file', (Join-Path $root 'backend/Dockerfile'), (Join-Path $root 'backend')) -FailureMessage 'API image build failed.'
    Invoke-AcrBuild -RegistryName $registryName -BuildArguments @('--image', "maison-vigne-store:$ImageTag", '--build-arg', 'SITE=ops', '--build-arg', "VITE_API_BASE_URL=$apiUrl", '--file', (Join-Path $root 'frontend/Dockerfile'), (Join-Path $root 'frontend')) -FailureMessage 'Store image build failed.'
    Invoke-AcrBuild -RegistryName $registryName -BuildArguments @('--image', "maison-vigne-admin:$ImageTag", '--build-arg', "VITE_API_BASE_URL=$apiUrl", '--build-arg', "VITE_APPLICATIONINSIGHTS_CONNECTION_STRING_BASE64=$applicationInsightsConnectionStringBase64", '--file', (Join-Path $root 'admin-frontend/Dockerfile'), (Join-Path $root 'admin-frontend')) -FailureMessage 'Admin image build failed.'

    Write-Parameters -Tag $ImageTag
    az deployment sub create --name "$deploymentName-apps" --location $Location --template-file $template --parameters "@$temporaryParameters" --no-wait --only-show-errors --output none
    if ($LASTEXITCODE -ne 0) { throw 'Application deployment failed.' }
    az deployment sub wait --name "$deploymentName-apps" --created --only-show-errors
    if ($LASTEXITCODE -ne 0) { throw 'Application deployment wait failed.' }
    $final = az deployment sub show --name "$deploymentName-apps" --query properties.outputs -o json --only-show-errors | ConvertFrom-Json
    if ($LASTEXITCODE -ne 0) { throw 'Application deployment output retrieval failed.' }
    [pscustomobject]@{
        ResourceGroup = $final.resourceGroupName.value
        Store = $final.operationsUrl.value
        Admin = $final.adminUrl.value
        Api = $final.apiUrl.value
        SreAgent = $final.sreAgentName.value
        ImageTag = $ImageTag
    }
}
finally {
    if (Test-Path $temporaryParameters) { Remove-Item $temporaryParameters -Force }
    $postgresPassword = $null
    $applicationInsightsConnectionString = $null
    $applicationInsightsConnectionStringBase64 = $null
}