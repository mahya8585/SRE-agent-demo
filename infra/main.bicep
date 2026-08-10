@description('Azure region validated for Container Apps and Container Registry availability.')
param location string = 'japaneast'

@minLength(2)
@maxLength(12)
@description('Short environment label used to create deterministic resource names.')
param environmentName string = 'demo'

@description('Frontend operations image. Leave empty to deploy the Container Apps bootstrap image.')
param opsImage string = ''

@description('Operation Pulse image. Leave empty to deploy the Container Apps bootstrap image.')
param pulseImage string = ''

@description('Spring Boot API image. Leave empty to deploy the Container Apps bootstrap image.')
param apiImage string = ''

@minValue(1)
@maxValue(65535)
param opsTargetPort int = 80

@minValue(1)
@maxValue(65535)
param pulseTargetPort int = 80

@minValue(1)
@maxValue(65535)
param apiTargetPort int = 80

var resourceToken = uniqueString(subscription().id, resourceGroup().id, location, environmentName)
var bootstrapImage = 'mcr.microsoft.com/azuredocs/containerapps-helloworld:latest'

resource logAnalytics 'Microsoft.OperationalInsights/workspaces@2022-10-01' = {
  name: 'azlog${resourceToken}'
  location: location
  properties: {
    retentionInDays: 30
    features: {
      enableLogAccessUsingOnlyResourcePermissions: true
    }
  }
}

resource registry 'Microsoft.ContainerRegistry/registries@2023-07-01' = {
  name: 'azcr${resourceToken}'
  location: location
  sku: {
    name: 'Basic'
  }
  properties: {
    adminUserEnabled: false
    publicNetworkAccess: 'Enabled'
    policies: {
      quarantinePolicy: {
        status: 'disabled'
      }
      retentionPolicy: {
        days: 7
        status: 'disabled'
      }
      trustPolicy: {
        type: 'Notary'
        status: 'disabled'
      }
    }
  }
}

resource containerIdentity 'Microsoft.ManagedIdentity/userAssignedIdentities@2023-01-31' = {
  name: 'azid${resourceToken}'
  location: location
}

resource acrPullRole 'Microsoft.Authorization/roleAssignments@2022-04-01' = {
  name: guid(registry.id, containerIdentity.id, 'AcrPull')
  scope: registry
  properties: {
    principalId: containerIdentity.properties.principalId
    principalType: 'ServicePrincipal'
    roleDefinitionId: subscriptionResourceId('Microsoft.Authorization/roleDefinitions', '7f951dda-4ed3-4680-a7ca-43fe172d538d')
  }
}

resource containerAppEnvironment 'Microsoft.App/managedEnvironments@2024-03-01' = {
  name: 'azcae${resourceToken}'
  location: location
  properties: {
    appLogsConfiguration: {
      destination: 'log-analytics'
      logAnalyticsConfiguration: {
        customerId: logAnalytics.properties.customerId
        sharedKey: logAnalytics.listKeys().primarySharedKey
      }
    }
    zoneRedundant: false
  }
}

resource opsApp 'Microsoft.App/containerApps@2024-03-01' = {
  name: 'azops${resourceToken}'
  location: location
  identity: {
    type: 'UserAssigned'
    userAssignedIdentities: {
      '${containerIdentity.id}': {}
    }
  }
  properties: {
    managedEnvironmentId: containerAppEnvironment.id
    configuration: {
      activeRevisionsMode: 'Single'
      registries: [
        {
          server: registry.properties.loginServer
          identity: containerIdentity.id
        }
      ]
      ingress: {
        external: true
        targetPort: opsTargetPort
        transport: 'auto'
        allowInsecure: false
        corsPolicy: {
          allowedOrigins: [
            '*'
          ]
          allowedMethods: [
            'GET'
          ]
          allowedHeaders: [
            '*'
          ]
        }
      }
    }
    template: {
      containers: [
        {
          name: 'ops'
          image: empty(opsImage) ? bootstrapImage : opsImage
          resources: {
            cpu: json('0.25')
            memory: '0.5Gi'
          }
        }
      ]
      scale: {
        minReplicas: 0
        maxReplicas: 2
      }
    }
  }
  dependsOn: [
    acrPullRole
  ]
}

resource pulseApp 'Microsoft.App/containerApps@2024-03-01' = {
  name: 'azpul${resourceToken}'
  location: location
  identity: {
    type: 'UserAssigned'
    userAssignedIdentities: {
      '${containerIdentity.id}': {}
    }
  }
  properties: {
    managedEnvironmentId: containerAppEnvironment.id
    configuration: {
      activeRevisionsMode: 'Single'
      registries: [
        {
          server: registry.properties.loginServer
          identity: containerIdentity.id
        }
      ]
      ingress: {
        external: true
        targetPort: pulseTargetPort
        transport: 'auto'
        allowInsecure: false
        corsPolicy: {
          allowedOrigins: [
            '*'
          ]
          allowedMethods: [
            'GET'
          ]
          allowedHeaders: [
            '*'
          ]
        }
      }
    }
    template: {
      containers: [
        {
          name: 'pulse'
          image: empty(pulseImage) ? bootstrapImage : pulseImage
          resources: {
            cpu: json('0.25')
            memory: '0.5Gi'
          }
        }
      ]
      scale: {
        minReplicas: 0
        maxReplicas: 2
      }
    }
  }
  dependsOn: [
    acrPullRole
  ]
}

resource apiApp 'Microsoft.App/containerApps@2024-03-01' = {
  name: 'azapi${resourceToken}'
  location: location
  identity: {
    type: 'UserAssigned'
    userAssignedIdentities: {
      '${containerIdentity.id}': {}
    }
  }
  properties: {
    managedEnvironmentId: containerAppEnvironment.id
    configuration: {
      activeRevisionsMode: 'Single'
      registries: [
        {
          server: registry.properties.loginServer
          identity: containerIdentity.id
        }
      ]
      ingress: {
        external: true
        targetPort: apiTargetPort
        transport: 'auto'
        allowInsecure: false
        corsPolicy: {
          allowedOrigins: [
            'https://${opsApp.properties.configuration.ingress.fqdn}'
            'https://${pulseApp.properties.configuration.ingress.fqdn}'
          ]
          allowedMethods: [
            'GET'
            'POST'
            'OPTIONS'
          ]
          allowedHeaders: [
            '*'
          ]
        }
      }
    }
    template: {
      containers: [
        {
          name: 'api'
          image: empty(apiImage) ? bootstrapImage : apiImage
          env: [
            {
              name: 'CORS_ALLOWED_ORIGINS'
              value: 'https://${opsApp.properties.configuration.ingress.fqdn},https://${pulseApp.properties.configuration.ingress.fqdn}'
            }
          ]
          resources: {
            cpu: json('0.5')
            memory: '1Gi'
          }
        }
      ]
      scale: {
        minReplicas: 0
        maxReplicas: 2
      }
    }
  }
  dependsOn: [
    acrPullRole
  ]
}

output registryName string = registry.name
output registryLoginServer string = registry.properties.loginServer
output operationsUrl string = 'https://${opsApp.properties.configuration.ingress.fqdn}'
output pulseUrl string = 'https://${pulseApp.properties.configuration.ingress.fqdn}'
output apiUrl string = 'https://${apiApp.properties.configuration.ingress.fqdn}'
