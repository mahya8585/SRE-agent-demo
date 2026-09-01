package com.example.wine.telemetry;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class StartupFailureSuppressionPolicy {
    private final Duration startupFailureSuppressionWindow;
    private final Clock clock;
    private final Instant startupStartedAt;
    private final Set<String> suppressedFailureChains = ConcurrentHashMap.newKeySet();
    private volatile Instant readyAt;

    @Autowired
    public StartupFailureSuppressionPolicy(
            @Value("${app.telemetry.startup-failure-suppression-window:PT1M}") Duration startupFailureSuppressionWindow) {
        this(startupFailureSuppressionWindow, Clock.systemUTC());
    }

    StartupFailureSuppressionPolicy(Duration startupFailureSuppressionWindow, Clock clock) {
        this.startupFailureSuppressionWindow = startupFailureSuppressionWindow;
        this.clock = clock;
        this.startupStartedAt = Instant.now(clock);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        readyAt = Instant.now(clock);
        suppressedFailureChains.clear();
    }

    boolean shouldSuppressPostgresFailure(Throwable throwable) {
        return !isReady()
                && Instant.now(clock).isBefore(startupStartedAt.plus(startupFailureSuppressionWindow))
                && StartupFailureTelemetryListener.containsPostgresConnectionException(throwable);
    }

    boolean tryRecordSuppressedFailure(String failureChain) {
        return suppressedFailureChains.add(failureChain);
    }

    private boolean isReady() {
        return readyAt != null;
    }
}
