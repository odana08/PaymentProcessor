package org.example.dto;

import java.util.List;

public record PaymentOptionsResponse(
        List<String> paymentTypes,
        List<String> notificationChannels
) {
}
