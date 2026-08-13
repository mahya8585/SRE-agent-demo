param location string
param resourceToken string
param tags object

var monitoringReaderRoleDefinitionId = subscriptionResourceId('Microsoft.Authorization/roleDefinitions', '43d0d8ad-25c7-4714-9337-8ba259a9fe05')
var logAnalyticsReaderRoleDefinitionId = subscriptionResourceId('Microsoft.Authorization/roleDefinitions', '73c42c96-874c-492b-b04d-ab87d138a893')

resource monitoringAccount 'Microsoft.Monitor/accounts@2025-10-03' = {
  name: 'azmon${resourceToken}'
  location: location
  tags: tags
  properties: {
    publicNetworkAccess: 'Enabled'
    metrics: {
      enableAccessUsingResourcePermissions: true
    }
  }
}

resource agent 'Microsoft.Monitor/observabilityAgents@2026-05-01-preview' = {
  name: 'azsre${resourceToken}'
  location: location
  tags: tags
  identity: {
    type: 'SystemAssigned'
  }
  properties: {
    enabled: true
    monitoringAccountId: monitoringAccount.id
    operations: [
      {
        type: 'IssueCreation'
        mode: 'Manual'
        instructions: 'Monitor the Maison Vigne production workload and create evidence-based issues for actionable reliability risks.'
      }
      {
        type: 'Investigation'
        mode: 'Manual'
        instructions: 'Investigate incidents using Azure Monitor evidence. Propose mitigations, but require human approval before any change.'
      }
    ]
  }
}

resource monitoringReader 'Microsoft.Authorization/roleAssignments@2022-04-01' = {
  name: guid(resourceGroup().id, agent.id, monitoringReaderRoleDefinitionId)
  properties: {
    roleDefinitionId: monitoringReaderRoleDefinitionId
    principalId: agent.identity.principalId
    principalType: 'ServicePrincipal'
  }
}

resource logAnalyticsReader 'Microsoft.Authorization/roleAssignments@2022-04-01' = {
  name: guid(resourceGroup().id, agent.id, logAnalyticsReaderRoleDefinitionId)
  properties: {
    roleDefinitionId: logAnalyticsReaderRoleDefinitionId
    principalId: agent.identity.principalId
    principalType: 'ServicePrincipal'
  }
}

output agentName string = agent.name
output agentId string = agent.id
