param location string
@minLength(1)
param resourceToken string
param deploymentPrincipalId string
param privateEndpointSubnetId string
param virtualNetworkId string
param tags object

resource logAnalytics 'Microsoft.OperationalInsights/workspaces@2023-09-01' = {
  name: 'azlog${resourceToken}'
  location: location
  tags: tags
  properties: {
    retentionInDays: 30
    workspaceCapping: { dailyQuotaGb: 1 }
    features: { enableLogAccessUsingOnlyResourcePermissions: true }
  }
}

resource appInsights 'Microsoft.Insights/components@2020-02-02' = {
  name: 'azai${resourceToken}'
  location: location
  kind: 'web'
  tags: tags
  properties: { Application_Type: 'web', WorkspaceResourceId: logAnalytics.id }
}

resource registry 'Microsoft.ContainerRegistry/registries@2023-07-01' = {
  name: 'azcr${resourceToken}'
  location: location
  tags: tags
  sku: { name: 'Basic' }
  properties: { adminUserEnabled: false, publicNetworkAccess: 'Enabled' }
}

resource pullIdentity 'Microsoft.ManagedIdentity/userAssignedIdentities@2023-01-31' = {
  name: 'azidpull${resourceToken}'
  location: location
  tags: tags
}
resource apiIdentity 'Microsoft.ManagedIdentity/userAssignedIdentities@2023-01-31' = {
  name: 'azidapi${resourceToken}'
  location: location
  tags: tags
}

resource storage 'Microsoft.Storage/storageAccounts@2023-05-01' = {
  name: 'azst${resourceToken}'
  location: location
  tags: tags
  sku: { name: 'Standard_ZRS' }
  kind: 'StorageV2'
  properties: {
    accessTier: 'Hot'
    allowBlobPublicAccess: false
    allowSharedKeyAccess: false
    defaultToOAuthAuthentication: true
    minimumTlsVersion: 'TLS1_2'
    publicNetworkAccess: 'Disabled'
    supportsHttpsTrafficOnly: true
    networkAcls: {
      bypass: 'None'
      defaultAction: 'Deny'
    }
  }
}

resource blobService 'Microsoft.Storage/storageAccounts/blobServices@2023-05-01' = {
  parent: storage
  name: 'default'
  properties: {
    containerDeleteRetentionPolicy: { enabled: true, days: 7 }
    deleteRetentionPolicy: { enabled: true, days: 7 }
  }
}

resource wineImagesContainer 'Microsoft.Storage/storageAccounts/blobServices/containers@2023-05-01' = {
  parent: blobService
  name: 'wine-images'
  properties: { publicAccess: 'None' }
}

resource apiBlobDataContributorRole 'Microsoft.Authorization/roleAssignments@2022-04-01' = {
  name: guid(storage.id, apiIdentity.id, 'StorageBlobDataContributor')
  scope: storage
  properties: {
    principalId: apiIdentity.properties.principalId
    principalType: 'ServicePrincipal'
    roleDefinitionId: subscriptionResourceId('Microsoft.Authorization/roleDefinitions', 'ba92f5b4-2d11-453d-a403-e96b0029c9fe')
  }
}

resource blobPrivateDns 'Microsoft.Network/privateDnsZones@2024-06-01' = {
  name: 'privatelink.blob.${environment().suffixes.storage}'
  location: 'global'
  tags: tags
}

resource blobPrivateDnsLink 'Microsoft.Network/privateDnsZones/virtualNetworkLinks@2024-06-01' = {
  parent: blobPrivateDns
  name: 'azstlink${resourceToken}'
  location: 'global'
  tags: tags
  properties: {
    registrationEnabled: false
    virtualNetwork: { id: virtualNetworkId }
  }
}

resource blobPrivateEndpoint 'Microsoft.Network/privateEndpoints@2024-05-01' = {
  name: 'azpest${resourceToken}'
  location: location
  tags: tags
  properties: {
    subnet: { id: privateEndpointSubnetId }
    privateLinkServiceConnections: [
      {
        name: 'blob-storage'
        properties: {
          privateLinkServiceId: storage.id
          groupIds: [ 'blob' ]
        }
      }
    ]
  }
}

resource blobDnsZoneGroup 'Microsoft.Network/privateEndpoints/privateDnsZoneGroups@2024-05-01' = {
  parent: blobPrivateEndpoint
  name: 'default'
  properties: {
    privateDnsZoneConfigs: [
      {
        name: 'blob-storage'
        properties: { privateDnsZoneId: blobPrivateDns.id }
      }
    ]
  }
}

resource acrPullRole 'Microsoft.Authorization/roleAssignments@2022-04-01' = {
  name: guid(registry.id, pullIdentity.id, 'AcrPull')
  scope: registry
  properties: {
    principalId: pullIdentity.properties.principalId
    principalType: 'ServicePrincipal'
    roleDefinitionId: subscriptionResourceId('Microsoft.Authorization/roleDefinitions', '7f951dda-4ed3-4680-a7ca-43fe172d538d')
  }
}

resource keyVault 'Microsoft.KeyVault/vaults@2023-07-01' = {
  name: 'azkv${resourceToken}'
  location: location
  tags: tags
  properties: {
    tenantId: tenant().tenantId
    sku: { family: 'A', name: 'standard' }
    enableRbacAuthorization: true
    enablePurgeProtection: true
    softDeleteRetentionInDays: 90
    publicNetworkAccess: 'Disabled'
  }
}

resource keyVaultPrivateDns 'Microsoft.Network/privateDnsZones@2024-06-01' = {
  name: 'privatelink.vaultcore.azure.net'
  location: 'global'
  tags: tags
}

resource keyVaultPrivateDnsLink 'Microsoft.Network/privateDnsZones/virtualNetworkLinks@2024-06-01' = {
  parent: keyVaultPrivateDns
  name: 'azkvlink${resourceToken}'
  location: 'global'
  tags: tags
  properties: {
    registrationEnabled: false
    virtualNetwork: { id: virtualNetworkId }
  }
}

resource keyVaultPrivateEndpoint 'Microsoft.Network/privateEndpoints@2024-05-01' = {
  name: 'azpekv${resourceToken}'
  location: location
  tags: tags
  properties: {
    subnet: { id: privateEndpointSubnetId }
    privateLinkServiceConnections: [
      {
        name: 'key-vault'
        properties: {
          privateLinkServiceId: keyVault.id
          groupIds: [ 'vault' ]
        }
      }
    ]
  }
}

resource keyVaultDnsZoneGroup 'Microsoft.Network/privateEndpoints/privateDnsZoneGroups@2024-05-01' = {
  parent: keyVaultPrivateEndpoint
  name: 'default'
  properties: {
    privateDnsZoneConfigs: [
      {
        name: 'key-vault'
        properties: { privateDnsZoneId: keyVaultPrivateDns.id }
      }
    ]
  }
}

resource apiSecretsRole 'Microsoft.Authorization/roleAssignments@2022-04-01' = {
  name: guid(keyVault.id, apiIdentity.id, 'KeyVaultSecretsUser')
  scope: keyVault
  properties: {
    principalId: apiIdentity.properties.principalId
    principalType: 'ServicePrincipal'
    roleDefinitionId: subscriptionResourceId('Microsoft.Authorization/roleDefinitions', '4633458b-17de-408a-b874-0445c86b69e6')
  }
}
resource deployerSecretsRole 'Microsoft.Authorization/roleAssignments@2022-04-01' = {
  name: guid(keyVault.id, deploymentPrincipalId, 'KeyVaultSecretsOfficer')
  scope: keyVault
  properties: {
    principalId: deploymentPrincipalId
    roleDefinitionId: subscriptionResourceId('Microsoft.Authorization/roleDefinitions', 'b86a8fe4-44ce-4948-aee5-eccb2c155cd7')
  }
}

output logAnalyticsName string = logAnalytics.name
output applicationInsightsConnectionString string = appInsights.properties.ConnectionString
output registryName string = registry.name
output registryLoginServer string = registry.properties.loginServer
output pullIdentityId string = pullIdentity.id
output apiIdentityId string = apiIdentity.id
output apiIdentityClientId string = apiIdentity.properties.clientId
output keyVaultName string = keyVault.name
output storageAccountName string = storage.name
output blobEndpoint string = storage.properties.primaryEndpoints.blob
output wineImagesContainerName string = wineImagesContainer.name
