param location string
param resourceToken string
param databaseSubnetId string
param virtualNetworkId string
param postgresAdministratorLogin string
@secure()
param postgresAdministratorPassword string
param keyVaultName string
param tags object

var serverName = 'azpg${resourceToken}'
var databaseName = 'winedb'

resource privateDnsZone 'Microsoft.Network/privateDnsZones@2024-06-01' = {
  name: '${serverName}.private.postgres.database.azure.com'
  location: 'global'
  tags: tags
}
resource privateDnsLink 'Microsoft.Network/privateDnsZones/virtualNetworkLinks@2024-06-01' = {
  parent: privateDnsZone
  name: 'azlnk${resourceToken}'
  location: 'global'
  tags: tags
  properties: {
    registrationEnabled: false
    virtualNetwork: { id: virtualNetworkId }
  }
}
resource postgres 'Microsoft.DBforPostgreSQL/flexibleServers@2024-08-01' = {
  name: serverName
  location: location
  tags: tags
  sku: { name: 'Standard_B1ms', tier: 'Burstable' }
  properties: {
    administratorLogin: postgresAdministratorLogin
    administratorLoginPassword: postgresAdministratorPassword
    version: '17'
    storage: { storageSizeGB: 32 }
    backup: { backupRetentionDays: 7, geoRedundantBackup: 'Disabled' }
    highAvailability: { mode: 'Disabled' }
    network: {
      delegatedSubnetResourceId: databaseSubnetId
      privateDnsZoneArmResourceId: privateDnsZone.id
      publicNetworkAccess: 'Disabled'
    }
  }
  dependsOn: [ privateDnsLink ]
}
resource database 'Microsoft.DBforPostgreSQL/flexibleServers/databases@2024-08-01' = {
  parent: postgres
  name: databaseName
  properties: { charset: 'UTF8', collation: 'en_US.utf8' }
}
resource keyVault 'Microsoft.KeyVault/vaults@2023-07-01' existing = { name: keyVaultName }
resource usernameSecret 'Microsoft.KeyVault/vaults/secrets@2023-07-01' = {
  parent: keyVault
  name: 'postgres-admin-username'
  properties: { value: postgresAdministratorLogin }
}
resource passwordSecret 'Microsoft.KeyVault/vaults/secrets@2023-07-01' = {
  parent: keyVault
  name: 'postgres-admin-password'
  properties: { value: postgresAdministratorPassword }
}

output serverName string = postgres.name
output serverFqdn string = postgres.properties.fullyQualifiedDomainName
output databaseName string = database.name
