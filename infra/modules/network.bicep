param location string
param resourceToken string
param tags object

resource databaseNsg 'Microsoft.Network/networkSecurityGroups@2024-05-01' = {
  name: 'aznsg${resourceToken}'
  location: location
  tags: tags
  properties: {
    securityRules: [
      {
        name: 'AllowContainerAppsPostgreSql'
        properties: {
          priority: 100
          access: 'Allow'
          direction: 'Inbound'
          protocol: 'Tcp'
          sourceAddressPrefix: '10.20.0.0/27'
          sourcePortRange: '*'
          destinationAddressPrefix: '10.20.1.0/28'
          destinationPortRange: '5432'
        }
      }
    ]
  }
}

resource virtualNetwork 'Microsoft.Network/virtualNetworks@2024-05-01' = {
  name: 'azvnt${resourceToken}'
  location: location
  tags: tags
  properties: {
    addressSpace: { addressPrefixes: [ '10.20.0.0/16' ] }
    subnets: [
      {
        name: 'azca${resourceToken}'
        properties: {
          addressPrefix: '10.20.0.0/27'
          delegations: [ { name: 'container-apps', properties: { serviceName: 'Microsoft.App/environments' } } ]
        }
      }
      {
        name: 'azdb${resourceToken}'
        properties: {
          addressPrefix: '10.20.1.0/28'
          networkSecurityGroup: { id: databaseNsg.id }
          delegations: [ { name: 'postgres', properties: { serviceName: 'Microsoft.DBforPostgreSQL/flexibleServers' } } ]
        }
      }
      {
        name: 'azpe${resourceToken}'
        properties: {
          addressPrefix: '10.20.2.0/28'
          privateEndpointNetworkPolicies: 'Disabled'
        }
      }
    ]
  }
}

output containerAppsSubnetId string = resourceId('Microsoft.Network/virtualNetworks/subnets', virtualNetwork.name, 'azca${resourceToken}')
output databaseSubnetId string = resourceId('Microsoft.Network/virtualNetworks/subnets', virtualNetwork.name, 'azdb${resourceToken}')
output privateEndpointSubnetId string = resourceId('Microsoft.Network/virtualNetworks/subnets', virtualNetwork.name, 'azpe${resourceToken}')
output virtualNetworkId string = virtualNetwork.id
