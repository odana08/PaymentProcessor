package org.example.model;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public class Payment {

    private PaymentType type;
    private BigDecimal amountInCents;
    private final Currency currency;
    private final List<NotificationChannel> notificationChannels;
    private final UUID reference;
    private PaymentStatus status;


    public Payment(PaymentType type, BigDecimal amountInCents, Currency currency, NotificationChannel notificationChannel) {
        this(type, amountInCents, currency, List.of(notificationChannel));
    }

    public Payment(PaymentType type, BigDecimal amountInCents, Currency currency,
                   List<NotificationChannel> notificationChannels) {
        this.type = type;
        this.amountInCents = amountInCents;
        this.currency = currency;
        if (notificationChannels == null || notificationChannels.isEmpty()) {
            throw new IllegalArgumentException("At least one notification channel is required");
        }
        this.notificationChannels = List.copyOf(notificationChannels);
        this.reference = UUID.randomUUID();
        this.status = PaymentStatus.CREATED;
    }

    public PaymentType getType() {
        return type;
    }

    public BigDecimal getAmountInCents() {
        return amountInCents;
    }

    public Currency getCurrency() {
        return currency;
    }

    public NotificationChannel getNotificationChannel () {
        return notificationChannels.getFirst();
    }

    public List<NotificationChannel> getNotificationChannels() {
        return notificationChannels;
    }

    public UUID getReference() {
        return reference;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public void markAsProcessed() {
        status = PaymentStatus.PROCESSED;
    }
}
