package com.example.wine.telemetry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.boot.context.event.ApplicationFailedEvent;
import org.springframework.context.ApplicationListener;

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
        return ExceptionChainUtils.containsExceptionClass(throwable, "liquibase.exception.DatabaseException")
                && ExceptionChainUtils.containsExceptionClass(throwable, "org.springframework.beans.factory.BeanCreationException")
                && containsPostgresConnectionException(throwable);
    }

    private static boolean containsPostgresConnectionException(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if ("org.postgresql.util.PSQLException".equals(current.getClass().getName())
                    && current instanceof java.sql.SQLException) {
                String sqlState = ((java.sql.SQLException) current).getSQLState();
                if (sqlState != null && sqlState.startsWith("08")) {
                    return true;
                }
            }
            current = current.getCause();
        }
        return false;
    }

    static String summarizeExceptionChain(Throwable throwable) {
        return ExceptionChainUtils.summarizeExceptionChain(throwable);
    }
}
