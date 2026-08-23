package org.example.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "payments")
public class Payment {

    private static final int MONETARY_SCALE = 2;

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 36)
    private UUID reference;

    @Embedded
    private PaymentType type;

    @Column(name = "amount_in_cents", nullable = false, precision = 19, scale = 2)
    private BigDecimal amountInCents;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3)
    private Currency currency;

    @ElementCollection
    @CollectionTable(
            name = "payment_notification_channels",
            joinColumns = @JoinColumn(name = "payment_reference", nullable = false)
    )
    @OrderColumn(name = "channel_order")
    private List<NotificationChannel> notificationChannels = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    @Column(name = "fee_in_cents", nullable = false, precision = 19, scale = 2)
    private BigDecimal feeInCents;

    @Version
    @Column(nullable = false)
    private Long version;

    protected Payment() {
    }

    public Payment(PaymentType type, BigDecimal amountInCents, Currency currency,
                   NotificationChannel notificationChannel) {
        this(type, amountInCents, currency, List.of(notificationChannel));
    }

    public Payment(PaymentType type, BigDecimal amountInCents, Currency currency,
                   List<NotificationChannel> notificationChannels) {
        this.reference = UUID.randomUUID();
        replaceDetails(type, amountInCents, currency, notificationChannels);
        this.status = PaymentStatus.CREATED;
        this.feeInCents = BigDecimal.ZERO.setScale(MONETARY_SCALE);
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

    public List<NotificationChannel> getNotificationChannels() {
        return List.copyOf(notificationChannels);
    }

    public UUID getReference() {
        return reference;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public BigDecimal getFeeInCents() {
        return feeInCents;
    }

    public void setFeeInCents(BigDecimal feeInCents) {
        Objects.requireNonNull(feeInCents, "Fee cannot be null");
        if (feeInCents.signum() < 0) {
            throw new IllegalArgumentException("Fee cannot be negative");
        }
        this.feeInCents = feeInCents.setScale(MONETARY_SCALE, RoundingMode.HALF_UP);
    }

    public BigDecimal getTotalInCents() {
        return amountInCents.add(feeInCents);
    }

    public void replaceDetails(PaymentType type, BigDecimal amountInCents, Currency currency,
                               List<NotificationChannel> notificationChannels) {
        this.type = Objects.requireNonNull(type, "Payment type cannot be null");
        this.amountInCents = requirePositiveAmount(amountInCents);
        this.currency = Objects.requireNonNull(currency, "Currency cannot be null");
        replaceNotificationChannels(notificationChannels);
    }

    public void prepareForProcessing() {
        status = PaymentStatus.CREATED;
    }

    public void markAsProcessed() {
        status = PaymentStatus.PROCESSED;
    }

    private BigDecimal requirePositiveAmount(BigDecimal amount) {
        Objects.requireNonNull(amount, "Amount cannot be null");
        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
        if (amount.stripTrailingZeros().scale() > MONETARY_SCALE) {
            throw new IllegalArgumentException("Amount cannot have more than two decimal places");
        }
        return amount.setScale(MONETARY_SCALE);
    }

    private void replaceNotificationChannels(List<NotificationChannel> channels) {
        if (channels == null || channels.isEmpty()) {
            throw new IllegalArgumentException("At least one notification channel is required");
        }
        for (NotificationChannel channel : channels) {
            if (channel == null) {
                throw new IllegalArgumentException("Notification channels cannot contain null");
            }
        }
        notificationChannels.clear();
        notificationChannels.addAll(channels);
    }
}
