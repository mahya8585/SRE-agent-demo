package com.example.wine.telemetry;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Metrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

@Component
public class ApplicationTelemetry {
    private static final Logger logger = LoggerFactory.getLogger(ApplicationTelemetry.class);
    private static final Counter confirmedOrders = Metrics.counter("wine.orders.confirmed");
    private static final Counter stockRejections = Metrics.counter("wine.orders.stock_rejected");
    private static final DistributionSummary orderTotal = DistributionSummary.builder("wine.orders.total")
            .baseUnit("JPY")
            .register(Metrics.globalRegistry);
    private static final DistributionSummary orderItems = DistributionSummary.builder("wine.orders.items")
            .baseUnit("items")
            .register(Metrics.globalRegistry);

    public void orderConfirmed(String orderNumber, int itemCount, double total, boolean freeShipping) {
        confirmedOrders.increment();
        orderTotal.record(total);
        orderItems.record(itemCount);

        MDC.put("event.name", "OrderConfirmed");
        MDC.put("order.number", orderNumber);
        MDC.put("order.item_count", Integer.toString(itemCount));
        MDC.put("order.shipping_type", freeShipping ? "free" : "paid");
        try {
            logger.info("Order confirmed: itemCount={}, total={}, shippingType={}",
                    itemCount, total, freeShipping ? "free" : "paid");
        } finally {
            clearOrderContext();
        }
    }

    public void stockRejected(Long wineId, int requestedQuantity, int availableStock) {
        stockRejections.increment();

        MDC.put("event.name", "OrderRejectedOutOfStock");
        MDC.put("wine.id", Long.toString(wineId));
        MDC.put("stock.requested", Integer.toString(requestedQuantity));
        MDC.put("stock.available", Integer.toString(availableStock));
        try {
            logger.warn("Order rejected because stock is insufficient: wineId={}, requested={}, available={}",
                    wineId, requestedQuantity, availableStock);
        } finally {
            MDC.remove("event.name");
            MDC.remove("wine.id");
            MDC.remove("stock.requested");
            MDC.remove("stock.available");
        }
    }

    public void httpRequestFailed(int statusCode, String requestPath, Exception exception) {
        MDC.put("event.name", statusCode >= 500 ? "HttpRequestFailed" : "HttpRequestRejected");
        MDC.put("http.status_code", Integer.toString(statusCode));
        MDC.put("http.path", requestPath);
        MDC.put("exception.type", exception.getClass().getName());
        try {
            if (statusCode >= 500) {
                logger.error("HTTP request failed: status={}, path={}, exceptionType={}",
                        statusCode, requestPath, exception.getClass().getName(), exception);
            } else {
                logger.warn("HTTP request rejected: status={}, path={}, exceptionType={}",
                        statusCode, requestPath, exception.getClass().getName());
            }
        } finally {
            MDC.remove("event.name");
            MDC.remove("http.status_code");
            MDC.remove("http.path");
            MDC.remove("exception.type");
        }
    }

    private void clearOrderContext() {
        MDC.remove("event.name");
        MDC.remove("order.number");
        MDC.remove("order.item_count");
        MDC.remove("order.shipping_type");
    }
}