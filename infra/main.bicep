targetScope = 'subscription'

param location string = 'japaneast'
param resourceGroupName string = 'SREagent-lab'
param sreAgentLocation string = 'australiaeast'
@minLength(2)
@maxLength(12)
param environmentName string = 'prod'
param deploymentPrincipalId string
param postgresAdministratorLogin string
@secure()
param postgresAdministratorPassword string
param imageTag string = 'bootstrap'
param costCenter string = 'demo'

var resourceToken = uniqueString(subscription().id, location, environmentName, resourceGroupName)
var tags = {
  environment: environmentName
  application: 'maison-vigne'
  managedBy: 'bicep'
  costCenter: costCenter
}
var bootstrapImage = 'mcr.microsoft.com/azuredocs/containerapps-helloworld:latest'
var useBootstrap = imageTag == 'bootstrap'

resource resourceGroup 'Microsoft.Resources/resourceGroups@2024-03-01' = {
  name: resourceGroupName
  location: location
  tags: tags
}

module network 'modules/network.bicep' = {
  name: 'network-${resourceToken}'
  scope: resourceGroup
  params: { location: location, resourceToken: resourceToken, tags: tags }
}

module foundation 'modules/foundation.bicep' = {
  name: 'foundation-${resourceToken}'
  scope: resourceGroup
  params: {
    location: location
    resourceToken: resourceToken
    deploymentPrincipalId: deploymentPrincipalId
    privateEndpointSubnetId: network.outputs.privateEndpointSubnetId
    virtualNetworkId: network.outputs.virtualNetworkId
    tags: tags
  }
}

module database 'modules/database.bicep' = {
  name: 'database-${resourceToken}'
  scope: resourceGroup
  params: {
    location: location
    resourceToken: resourceToken
    databaseSubnetId: network.outputs.databaseSubnetId
    virtualNetworkId: network.outputs.virtualNetworkId
    postgresAdministratorLogin: postgresAdministratorLogin
    postgresAdministratorPassword: postgresAdministratorPassword
    keyVaultName: foundation.outputs.keyVaultName
    tags: tags
  }
}

module apps 'modules/container-apps.bicep' = {
  name: 'apps-${resourceToken}'
  scope: resourceGroup
  params: {
    location: location
    resourceToken: resourceToken
    containerAppsSubnetId: network.outputs.containerAppsSubnetId
    logAnalyticsName: foundation.outputs.logAnalyticsName
    registryName: foundation.outputs.registryName
    pullIdentityId: foundation.outputs.pullIdentityId
    apiIdentityId: foundation.outputs.apiIdentityId
    apiIdentityClientId: foundation.outputs.apiIdentityClientId
    keyVaultName: foundation.outputs.keyVaultName
    postgresServerFqdn: database.outputs.serverFqdn
    postgresDatabaseName: database.outputs.databaseName
    applicationInsightsConnectionString: foundation.outputs.applicationInsightsConnectionString
    blobEndpoint: foundation.outputs.blobEndpoint
    wineImagesContainerName: foundation.outputs.wineImagesContainerName
    storeImage: useBootstrap ? bootstrapImage : '${foundation.outputs.registryLoginServer}/maison-vigne-store:${imageTag}'
    apiImage: useBootstrap ? bootstrapImage : '${foundation.outputs.registryLoginServer}/wine-api:${imageTag}'
    bootstrapMode: useBootstrap
    targetPorts: useBootstrap ? { store: 80, api: 80 } : { store: 8080, api: 8081 }
    tags: tags
  }
}

module sreAgent 'modules/sre-agent.bicep' = {
  name: 'sre-agent-${resourceToken}'
  scope: resourceGroup
  params: {
    location: sreAgentLocation
    resourceToken: resourceToken
    tags: tags
  }
}

output resourceGroupName string = resourceGroup.name
output registryName string = foundation.outputs.registryName
output registryLoginServer string = foundation.outputs.registryLoginServer
output keyVaultName string = foundation.outputs.keyVaultName
output storageAccountName string = foundation.outputs.storageAccountName
output postgresServerName string = database.outputs.serverName
output operationsUrl string = apps.outputs.storeUrl
output apiUrl string = apps.outputs.apiUrl
output sreAgentName string = sreAgent.outputs.agentName
output sreAgentId string = sreAgent.outputs.agentId
