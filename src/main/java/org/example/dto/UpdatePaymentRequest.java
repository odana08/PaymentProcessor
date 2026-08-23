package org.example.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.example.model.Currency;

import java.math.BigDecimal;
import java.util.List;

public record UpdatePaymentRequest(
        @NotBlank @Size(max = 50) String paymentType,
        @NotNull @Positive @Digits(integer = 17, fraction = 2) BigDecimal amountInCents,
        @NotNull Currency currency,
        @NotEmpty @Size(max = 10) List<@NotBlank @Size(max = 32) String> notificationChannels
) {
}
