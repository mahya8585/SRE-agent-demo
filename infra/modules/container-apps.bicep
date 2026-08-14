param location string
param resourceToken string
param containerAppsSubnetId string
param logAnalyticsName string
param registryName string
param pullIdentityId string
param apiIdentityId string
param apiIdentityClientId string
param keyVaultName string
param postgresServerFqdn string
param postgresDatabaseName string
param applicationInsightsConnectionString string
param blobEndpoint string
param wineImagesContainerName string
param storeImage string
param apiImage string
param bootstrapMode bool
param targetPorts object
param tags object

resource logAnalytics 'Microsoft.OperationalInsights/workspaces@2023-09-01' existing = { name: logAnalyticsName }
resource registry 'Microsoft.ContainerRegistry/registries@2023-07-01' existing = { name: registryName }
resource keyVault 'Microsoft.KeyVault/vaults@2023-07-01' existing = { name: keyVaultName }

resource environment 'Microsoft.App/managedEnvironments@2024-03-01' = {
  name: 'azcae${resourceToken}'
  location: location
  tags: tags
  properties: {
    vnetConfiguration: { infrastructureSubnetId: containerAppsSubnetId, internal: false }
    appLogsConfiguration: {
      destination: 'log-analytics'
      logAnalyticsConfiguration: {
        customerId: logAnalytics.properties.customerId
        sharedKey: logAnalytics.listKeys().primarySharedKey
      }
    }
    zoneRedundant: false
    workloadProfiles: [ { name: 'Consumption', workloadProfileType: 'Consumption' } ]
  }
}

resource store 'Microsoft.App/containerApps@2024-03-01' = {
  name: 'azsto${resourceToken}'
  location: location
  tags: tags
  identity: { type: 'UserAssigned', userAssignedIdentities: { '${pullIdentityId}': {} } }
  properties: {
    managedEnvironmentId: environment.id
    workloadProfileName: 'Consumption'
    configuration: {
      activeRevisionsMode: 'Single'
      registries: [ { server: registry.properties.loginServer, identity: pullIdentityId } ]
      ingress: { external: true, targetPort: targetPorts.store, transport: 'auto', allowInsecure: false }
    }
    template: {
      containers: [ { name: 'store', image: storeImage, resources: { cpu: json('0.25'), memory: '0.5Gi' } } ]
      scale: { minReplicas: 0, maxReplicas: 2 }
    }
  }
}

resource api 'Microsoft.App/containerApps@2024-03-01' = {
  name: 'azapi${resourceToken}'
  location: location
  tags: tags
  identity: {
    type: 'UserAssigned'
    userAssignedIdentities: { '${pullIdentityId}': {}, '${apiIdentityId}': {} }
  }
  properties: {
    managedEnvironmentId: environment.id
    workloadProfileName: 'Consumption'
    configuration: {
      activeRevisionsMode: 'Single'
      registries: [ { server: registry.properties.loginServer, identity: pullIdentityId } ]
      secrets: bootstrapMode ? [] : [
        { name: 'postgres-admin-username', keyVaultUrl: '${keyVault.properties.vaultUri}secrets/postgres-admin-username', identity: apiIdentityId }
        { name: 'postgres-admin-password', keyVaultUrl: '${keyVault.properties.vaultUri}secrets/postgres-admin-password', identity: apiIdentityId }
      ]
      ingress: { external: true, targetPort: targetPorts.api, transport: 'auto', allowInsecure: false }
    }
    template: {
      containers: [
        {
          name: 'api'
          image: apiImage
          env: bootstrapMode ? [] : [
            { name: 'SPRING_PROFILES_ACTIVE', value: 'production' }
            { name: 'SPRING_DATASOURCE_URL', value: 'jdbc:postgresql://${postgresServerFqdn}:5432/${postgresDatabaseName}?sslmode=require' }
            { name: 'SPRING_DATASOURCE_USERNAME', secretRef: 'postgres-admin-username' }
            { name: 'SPRING_DATASOURCE_PASSWORD', secretRef: 'postgres-admin-password' }
            { name: 'CORS_ALLOWED_ORIGINS', value: 'https://${store.properties.configuration.ingress.fqdn}' }
            { name: 'APPLICATIONINSIGHTS_CONNECTION_STRING', value: applicationInsightsConnectionString }
            { name: 'AZURE_STORAGE_BLOB_ENDPOINT', value: blobEndpoint }
            { name: 'AZURE_STORAGE_BLOB_CONTAINER_NAME', value: wineImagesContainerName }
            { name: 'AZURE_CLIENT_ID', value: apiIdentityClientId }
          ]
          resources: { cpu: json('0.5'), memory: '1Gi' }
        }
      ]
      scale: { minReplicas: 0, maxReplicas: 2 }
    }
  }
}

output storeUrl string = 'https://${store.properties.configuration.ingress.fqdn}'
output apiUrl string = 'https://${api.properties.configuration.ingress.fqdn}'
