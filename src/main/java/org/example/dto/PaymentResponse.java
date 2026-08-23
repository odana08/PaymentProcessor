package org.example.dto;

import org.example.model.Currency;
import org.example.model.NotificationChannel;
import org.example.model.Payment;
import org.example.model.PaymentStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record PaymentResponse(
        UUID reference,
        String paymentType,
        BigDecimal amountInCents,
        Currency currency,
        List<String> notificationChannels,
        BigDecimal feeInCents,
        BigDecimal totalInCents,
        PaymentStatus status
) {

    public static PaymentResponse from(Payment payment) {
        List<String> channels = payment.getNotificationChannels().stream()
                .map(NotificationChannel::getName)
                .toList();

        return new PaymentResponse(
                payment.getReference(),
                payment.getType().getName(),
                payment.getAmountInCents(),
                payment.getCurrency(),
                channels,
                payment.getFeeInCents(),
                payment.getTotalInCents(),
                payment.getStatus()
        );
    }
}
