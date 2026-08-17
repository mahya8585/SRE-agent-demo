package com.example.wine.telemetry;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Metrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class ApplicationTelemetry {
    private static final Logger logger = LoggerFactory.getLogger(ApplicationTelemetry.class);
    private static final Counter confirmedOrders = Metrics.counter("wine.orders.confirmed");
    private static final Counter stockRejections = Metrics.counter("wine.orders.stock_rejected");
    private static final Counter adminOrderStatusUpdates = Metrics.counter("wine.admin.order_status_updates");
    private static final Counter adminInventoryUpdates = Metrics.counter("wine.admin.inventory_updates");
    private static final Counter adminWinesCreated = Metrics.counter("wine.admin.wines_created");
    private static final Counter adminPurchaseOrdersCreated = Metrics.counter("wine.admin.purchase_orders_created");
    private static final Counter adminPurchaseOrdersReceived = Metrics.counter("wine.admin.purchase_orders_received");
    private static final DistributionSummary orderTotal = DistributionSummary.builder("wine.orders.total")
            .baseUnit("JPY")
            .register(Metrics.globalRegistry);
    private static final DistributionSummary orderItems = DistributionSummary.builder("wine.orders.items")
            .baseUnit("items")
            .register(Metrics.globalRegistry);
    private final StartupFailureSuppressionPolicy startupFailureSuppressionPolicy;

    public ApplicationTelemetry(StartupFailureSuppressionPolicy startupFailureSuppressionPolicy) {
        this.startupFailureSuppressionPolicy = startupFailureSuppressionPolicy;
    }

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

    public void adminOrderStatusChanged(Long orderId, String previousStatus, String newStatus) {
        adminOrderStatusUpdates.increment();
        MDC.put("event.name", "AdminOrderStatusChanged");
        MDC.put("order.id", Long.toString(orderId));
        MDC.put("order.previous_status", previousStatus);
        MDC.put("order.new_status", newStatus);
        try {
            logger.info("Admin changed order status: orderId={}, previousStatus={}, newStatus={}",
                    orderId, previousStatus, newStatus);
        } finally {
            MDC.remove("event.name");
            MDC.remove("order.id");
            MDC.remove("order.previous_status");
            MDC.remove("order.new_status");
        }
    }

    public void adminInventoryUpdated(Long wineId, int previousStock, int newStock,
                                      int previousThreshold, int newThreshold) {
        adminInventoryUpdates.increment();
        MDC.put("event.name", "AdminInventoryUpdated");
        MDC.put("wine.id", Long.toString(wineId));
        MDC.put("stock.previous", Integer.toString(previousStock));
        MDC.put("stock.current", Integer.toString(newStock));
        MDC.put("threshold.previous", Integer.toString(previousThreshold));
        MDC.put("threshold.current", Integer.toString(newThreshold));
        try {
            logger.info("Admin updated inventory: wineId={}, stock={} -> {}, threshold={} -> {}",
                    wineId, previousStock, newStock, previousThreshold, newThreshold);
        } finally {
            MDC.remove("event.name");
            MDC.remove("wine.id");
            MDC.remove("stock.previous");
            MDC.remove("stock.current");
            MDC.remove("threshold.previous");
            MDC.remove("threshold.current");
        }
    }

    public void adminWineCreated(Long wineId, int initialStock) {
        adminWinesCreated.increment();
        MDC.put("event.name", "AdminWineCreated");
        MDC.put("wine.id", Long.toString(wineId));
        MDC.put("stock.initial", Integer.toString(initialStock));
        try {
            logger.info("Admin created wine: wineId={}, initialStock={}", wineId, initialStock);
        } finally {
            MDC.remove("event.name");
            MDC.remove("wine.id");
            MDC.remove("stock.initial");
        }
    }

    public void adminPurchaseOrderCreated(Long purchaseOrderId, Long wineId, int quantity,
                                          LocalDate deliveryDate) {
        adminPurchaseOrdersCreated.increment();
        MDC.put("event.name", "AdminPurchaseOrderCreated");
        MDC.put("purchase_order.id", Long.toString(purchaseOrderId));
        MDC.put("wine.id", Long.toString(wineId));
        MDC.put("purchase_order.quantity", Integer.toString(quantity));
        MDC.put("purchase_order.delivery_date", deliveryDate.toString());
        try {
            logger.info("Admin created purchase order: purchaseOrderId={}, wineId={}, quantity={}, deliveryDate={}",
                    purchaseOrderId, wineId, quantity, deliveryDate);
        } finally {
            clearPurchaseOrderContext();
        }
    }

    public void adminPurchaseOrderReceived(Long purchaseOrderId, Long wineId, int quantity,
                                           int previousStock, int newStock) {
        adminPurchaseOrdersReceived.increment();
        MDC.put("event.name", "AdminPurchaseOrderReceived");
        MDC.put("purchase_order.id", Long.toString(purchaseOrderId));
        MDC.put("wine.id", Long.toString(wineId));
        MDC.put("purchase_order.quantity", Integer.toString(quantity));
        MDC.put("stock.previous", Integer.toString(previousStock));
        MDC.put("stock.current", Integer.toString(newStock));
        try {
            logger.info("Admin received purchase order: purchaseOrderId={}, wineId={}, quantity={}, stock={} -> {}",
                    purchaseOrderId, wineId, quantity, previousStock, newStock);
        } finally {
            clearPurchaseOrderContext();
            MDC.remove("stock.previous");
            MDC.remove("stock.current");
        }
    }

    public void httpRequestFailed(int statusCode, String requestPath, Exception exception) {
        if (statusCode >= 500 && startupFailureSuppressionPolicy.shouldSuppressPostgresFailure(exception)) {
            String failureChain = ExceptionChainUtils.summarizeExceptionChain(exception);
            if (!startupFailureSuppressionPolicy.tryRecordSuppressedFailure(failureChain)) {
                return;
            }
            MDC.put("event.name", "HttpRequestSuppressedStartupFailure");
            MDC.put("http.status_code", Integer.toString(statusCode));
            MDC.put("http.path", requestPath);
            MDC.put("exception.type", exception.getClass().getName());
            MDC.put("startup.failure.severity", "expected_startup_failure");
            MDC.put("startup.failure.chain", failureChain);
            try {
                logger.warn("Suppressed repeated PostgreSQL failure during startup grace window: status={}, path={}, failureChain={}",
                        statusCode, requestPath, failureChain);
            } finally {
                MDC.remove("event.name");
                MDC.remove("http.status_code");
                MDC.remove("http.path");
                MDC.remove("exception.type");
                MDC.remove("startup.failure.severity");
                MDC.remove("startup.failure.chain");
            }
            return;
        }

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

    private void clearPurchaseOrderContext() {
        MDC.remove("event.name");
        MDC.remove("purchase_order.id");
        MDC.remove("wine.id");
        MDC.remove("purchase_order.quantity");
        MDC.remove("purchase_order.delivery_date");
    }
}