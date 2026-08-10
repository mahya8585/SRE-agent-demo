param location string = resourceGroup().location

resource containerAppEnvironment 'Microsoft.App/managedEnvironments@2024-03-01' = {
  name: 'wine-demo-env'
  location: location
  properties: {}
}

output environmentId string = containerAppEnvironment.id
