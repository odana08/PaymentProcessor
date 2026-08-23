package org.example.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.example.model.Currency;

import java.math.BigDecimal;
import java.util.List;

public record PatchPaymentRequest(
        @Size(min = 1, max = 50) String paymentType,
        @Positive @Digits(integer = 17, fraction = 2) BigDecimal amountInCents,
        Currency currency,
        @Size(min = 1, max = 10) List<@Size(min = 1, max = 32) String> notificationChannels
) {

    public boolean hasNoChanges() {
        return paymentType == null
                && amountInCents == null
                && currency == null
                && notificationChannels == null;
    }
}
