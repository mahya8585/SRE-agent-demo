package com.example.wine.telemetry;

import liquibase.exception.DatabaseException;
import org.junit.jupiter.api.Test;
import org.postgresql.util.PSQLState;
import org.postgresql.util.PSQLException;
import org.slf4j.MDC;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationFailedEvent;
import org.springframework.beans.factory.BeanCreationException;

import static org.assertj.core.api.Assertions.assertThat;

class StartupFailureTelemetryListenerTest {

    @Test
    void identifiesExpectedStartupConnectionFailureChain() {
        PSQLException postgresException = new PSQLException("The connection attempt failed.",
                PSQLState.CONNECTION_UNABLE_TO_CONNECT);
        DatabaseException liquibaseException = new DatabaseException(postgresException);
        BeanCreationException startupException = new BeanCreationException("liquibase", liquibaseException);

        assertThat(StartupFailureTelemetryListener.isExpectedStartupConnectionFailure(startupException)).isTrue();
        assertThat(StartupFailureTelemetryListener.summarizeExceptionChain(startupException))
                .isEqualTo(
                        "org.springframework.beans.factory.BeanCreationException -> liquibase.exception.DatabaseException -> org.postgresql.util.PSQLException");
    }

    @Test
    void doesNotTreatNonConnectionPostgresFailuresAsExpectedStartupFailures() {
        PSQLException postgresException = new PSQLException("Unique violation.", PSQLState.UNIQUE_VIOLATION);
        DatabaseException liquibaseException = new DatabaseException(postgresException);
        BeanCreationException startupException = new BeanCreationException("liquibase", liquibaseException);

        assertThat(StartupFailureTelemetryListener.isExpectedStartupConnectionFailure(startupException)).isFalse();
    }

    @Test
    void marksUnexpectedStartupFailuresAsNonExpected() {
        IllegalStateException startupException = new IllegalStateException("unexpected");

        assertThat(StartupFailureTelemetryListener.isExpectedStartupConnectionFailure(startupException)).isFalse();
        assertThat(StartupFailureTelemetryListener.summarizeExceptionChain(startupException))
                .isEqualTo("java.lang.IllegalStateException");
    }

    @Test
    void clearsMdcAfterHandlingStartupFailureEvent() {
        StartupFailureTelemetryListener listener = new StartupFailureTelemetryListener();
        ApplicationFailedEvent event = new ApplicationFailedEvent(
                new SpringApplication(Object.class),
                new String[0],
                null,
                new IllegalStateException("unexpected"));

        listener.onApplicationEvent(event);

        assertThat(MDC.get("event.name")).isNull();
        assertThat(MDC.get("startup.failure.severity")).isNull();
        assertThat(MDC.get("startup.failure.chain")).isNull();
    }
}
