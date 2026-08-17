package com.example.wine.telemetry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.boot.context.event.ApplicationFailedEvent;
import org.springframework.context.ApplicationListener;

import java.util.ArrayList;
import java.util.List;

public class StartupFailureTelemetryListener implements ApplicationListener<ApplicationFailedEvent> {
    private static final Logger logger = LoggerFactory.getLogger(StartupFailureTelemetryListener.class);

    @Override
    public void onApplicationEvent(ApplicationFailedEvent event) {
        Throwable failure = event.getException();
        if (failure == null) {
            return;
        }

        String exceptionChain = summarizeExceptionChain(failure);
        String severity = isExpectedStartupConnectionFailure(failure) ? "expected_startup_failure" : "unexpected_startup_failure";

        MDC.put("event.name", "ApplicationStartupFailed");
        MDC.put("startup.failure.severity", severity);
        MDC.put("startup.failure.chain", exceptionChain);
        try {
            if ("expected_startup_failure".equals(severity)) {
                logger.warn("Application startup failed before readiness; startup failure was aggregated for alerting: {}",
                        exceptionChain);
            } else {
                logger.error("Application startup failed before readiness: {}", exceptionChain, failure);
            }
        } finally {
            MDC.remove("event.name");
            MDC.remove("startup.failure.severity");
            MDC.remove("startup.failure.chain");
        }
    }

    static boolean isExpectedStartupConnectionFailure(Throwable throwable) {
        List<String> chain = collectExceptionClassNames(throwable);
        return chain.contains("org.postgresql.util.PSQLException")
                && chain.contains("liquibase.exception.DatabaseException")
                && chain.contains("org.springframework.beans.factory.BeanCreationException");
    }

    static String summarizeExceptionChain(Throwable throwable) {
        return String.join(" -> ", collectExceptionClassNames(throwable));
    }

    private static List<String> collectExceptionClassNames(Throwable throwable) {
        List<String> names = new ArrayList<String>();
        Throwable current = throwable;
        while (current != null) {
            String name = current.getClass().getName();
            if (!names.contains(name)) {
                names.add(name);
            }
            current = current.getCause();
        }
        return names;
    }
}
