import { ApplicationInsights } from '@microsoft/applicationinsights-web';

const connectionString = import.meta.env.VITE_APPLICATIONINSIGHTS_CONNECTION_STRING;
let applicationInsights;

if (connectionString) {
  applicationInsights = new ApplicationInsights({
    config: {
      connectionString,
      enableAutoRouteTracking: false,
      enableCorsCorrelation: true,
      enableRequestHeaderTracking: true,
      enableResponseHeaderTracking: true,
      disableCookiesUsage: true
    }
  });
  applicationInsights.loadAppInsights();
}

export const trackPageView = (name) => {
  applicationInsights?.trackPageView({ name });
};

export const trackEvent = (name, properties = {}) => {
  applicationInsights?.trackEvent({ name, properties });
};

export const trackApiFailure = (operation, status) => {
  applicationInsights?.trackEvent({
    name: 'AdminApiFailure',
    properties: { operation, status: String(status) }
  });
};