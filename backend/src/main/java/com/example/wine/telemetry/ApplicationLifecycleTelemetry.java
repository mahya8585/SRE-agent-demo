package com.example.wine.telemetry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class ApplicationLifecycleTelemetry {
    private static final Logger logger = LoggerFactory.getLogger(ApplicationLifecycleTelemetry.class);

    @EventListener(ApplicationReadyEvent.class)
    public void applicationReady() {
        logLifecycleEvent("ApplicationReady", "Application startup completed");
    }

    @EventListener(ContextClosedEvent.class)
    public void applicationStopping() {
        logLifecycleEvent("ApplicationStopping", "Application shutdown started");
    }

    private void logLifecycleEvent(String eventName, String message) {
        MDC.put("event.name", eventName);
        try {
            logger.info(message);
        } finally {
            MDC.remove("event.name");
        }
    }
}