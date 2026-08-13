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

try {
    Write-Parameters -Tag 'bootstrap'
    if ($ValidateOnly) {
        az deployment sub validate --name $deploymentName --location $Location --template-file $template --parameters "@$temporaryParameters" --only-show-errors | Out-Null
        if ($LASTEXITCODE -ne 0) { throw 'Azure subscription deployment validation failed.' }
        Write-Output 'Azure validation succeeded. No resources were created.'
        return
    }
    if ($WhatIf) {
        az deployment sub what-if --name $deploymentName --location $Location --template-file $template --parameters "@$temporaryParameters" --only-show-errors
        if ($LASTEXITCODE -ne 0) { throw 'Azure subscription deployment what-if failed.' }
        return
    }

    az deployment sub create --name "$deploymentName-bootstrap" --location $Location --template-file $template --parameters "@$temporaryParameters" --no-wait --only-show-errors --output none
    if ($LASTEXITCODE -ne 0) { throw 'Bootstrap deployment failed.' }
    az deployment sub wait --name "$deploymentName-bootstrap" --created --only-show-errors
    if ($LASTEXITCODE -ne 0) { throw 'Bootstrap deployment wait failed.' }
    $bootstrap = az deployment sub show --name "$deploymentName-bootstrap" --query properties.outputs -o json --only-show-errors | ConvertFrom-Json
    if ($LASTEXITCODE -ne 0) { throw 'Bootstrap deployment output retrieval failed.' }
    $resourceGroupName = $bootstrap.resourceGroupName.value
    $registryName = $bootstrap.registryName.value
    $apiUrl = $bootstrap.apiUrl.value

    Invoke-AcrBuild -RegistryName $registryName -BuildArguments @('--image', "wine-api:$ImageTag", '--file', (Join-Path $root 'backend/Dockerfile'), (Join-Path $root 'backend')) -FailureMessage 'API image build failed.'
    Invoke-AcrBuild -RegistryName $registryName -BuildArguments @('--image', "maison-vigne-store:$ImageTag", '--build-arg', 'SITE=ops', '--build-arg', "VITE_API_BASE_URL=$apiUrl", '--file', (Join-Path $root 'frontend/Dockerfile'), (Join-Path $root 'frontend')) -FailureMessage 'Store image build failed.'

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
        Api = $final.apiUrl.value
        SreAgent = $final.sreAgentName.value
        ImageTag = $ImageTag
    }
}
finally {
    if (Test-Path $temporaryParameters) { Remove-Item $temporaryParameters -Force }
    $postgresPassword = $null
}