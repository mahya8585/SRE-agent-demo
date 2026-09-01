package com.example.wine.telemetry;

import liquibase.exception.DatabaseException;
import org.junit.jupiter.api.Test;
import org.postgresql.util.PSQLException;
import org.postgresql.util.PSQLState;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class StartupFailureSuppressionPolicyTest {

    @Test
    void canBeCreatedBySpringWhenTestConstructorAlsoExists() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.getBeanFactory().setConversionService(ApplicationConversionService.getSharedInstance());
            context.register(StartupFailureSuppressionPolicy.class);
            context.refresh();

            assertThat(context.getBean(StartupFailureSuppressionPolicy.class)).isNotNull();
        }
    }

    @Test
    void suppressesPostgresFailureBeforeReadyWithinSuppressionWindow() {
        StartupFailureSuppressionPolicy policy = new StartupFailureSuppressionPolicy(
                Duration.ofMinutes(1),
                Clock.fixed(Instant.parse("2026-08-17T00:00:00Z"), ZoneOffset.UTC));

        assertThat(policy.shouldSuppressPostgresFailure(startupFailure())).isTrue();
    }

    @Test
    void doesNotSuppressNonConnectionPostgresFailures() {
        StartupFailureSuppressionPolicy policy = new StartupFailureSuppressionPolicy(
                Duration.ofMinutes(1),
                Clock.fixed(Instant.parse("2026-08-17T00:00:00Z"), ZoneOffset.UTC));

        PSQLException postgresException = new PSQLException("Unique violation.", PSQLState.UNIQUE_VIOLATION);
        DatabaseException liquibaseException = new DatabaseException(postgresException);
        Throwable failure = new BeanCreationException("liquibase", liquibaseException);

        assertThat(policy.shouldSuppressPostgresFailure(failure)).isFalse();
    }

    @Test
    void doesNotSuppressAfterApplicationReady() {
        StartupFailureSuppressionPolicy policy = new StartupFailureSuppressionPolicy(
                Duration.ofMinutes(1),
                Clock.fixed(Instant.parse("2026-08-17T00:00:00Z"), ZoneOffset.UTC));
        policy.onApplicationReady();

        assertThat(policy.shouldSuppressPostgresFailure(startupFailure())).isFalse();
    }

    @Test
    void aggregatesSameSuppressedFailureChainOnce() {
        StartupFailureSuppressionPolicy policy = new StartupFailureSuppressionPolicy(
                Duration.ofMinutes(1),
                Clock.fixed(Instant.parse("2026-08-17T00:00:00Z"), ZoneOffset.UTC));
        String chain = "org.springframework.beans.factory.BeanCreationException -> liquibase.exception.DatabaseException -> org.postgresql.util.PSQLException";

        assertThat(policy.tryRecordSuppressedFailure(chain)).isTrue();
        assertThat(policy.tryRecordSuppressedFailure(chain)).isFalse();
    }

    @Test
    void doesNotSuppressWhenSuppressionWindowHasElapsed() {
        StartupFailureSuppressionPolicy policy = new StartupFailureSuppressionPolicy(
                Duration.ZERO,
                Clock.fixed(Instant.parse("2026-08-17T00:00:00Z"), ZoneOffset.UTC));

        assertThat(policy.shouldSuppressPostgresFailure(startupFailure())).isFalse();
    }

    private static Throwable startupFailure() {
        PSQLException postgresException = new PSQLException("The connection attempt failed.",
                PSQLState.CONNECTION_UNABLE_TO_CONNECT);
        DatabaseException liquibaseException = new DatabaseException(postgresException);
        return new BeanCreationException("liquibase", liquibaseException);
    }
}
