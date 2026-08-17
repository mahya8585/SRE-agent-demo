package com.example.wine.telemetry;

import liquibase.exception.DatabaseException;
import org.junit.jupiter.api.Test;
import org.postgresql.util.PSQLState;
import org.postgresql.util.PSQLException;
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
    void marksUnexpectedStartupFailuresAsNonExpected() {
        IllegalStateException startupException = new IllegalStateException("unexpected");

        assertThat(StartupFailureTelemetryListener.isExpectedStartupConnectionFailure(startupException)).isFalse();
    }
}
