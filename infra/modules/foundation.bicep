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
output keyVaultName string = keyVault.name
