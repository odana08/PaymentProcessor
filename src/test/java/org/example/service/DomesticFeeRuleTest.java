package org.example.service;

import org.example.model.NotificationChannel;
import org.example.model.Currency;
import org.example.model.Payment;
import org.example.model.PaymentType;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class DomesticFeeRuleTest {

    private final DomesticFeeRule rule = new DomesticFeeRule();

    @Test
    void givenDomesticFeeRule_whenSupportedTypeRequested_thenReturnsDomesticFee() {
        PaymentType supportedType = rule.supportedType();

        assertEquals("DOMESTIC_FEE", supportedType.getName());
    }

    @Test
    void givenAnyPayment_whenFeeCalculated_thenReturnsFixedFee() {
        Payment payment = new Payment(
                new PaymentType("DOMESTIC_FEE"),
                new BigDecimal("10000"),
                Currency.JOD,
                new NotificationChannel("EMAIL")
        );

        assertEquals(new BigDecimal("150"), rule.calculateFee(payment));
    }

    @Test
    void givenNullPayment_whenFeeCalculated_thenReturnsFixedFee() {
        assertEquals(new BigDecimal("150"), rule.calculateFee(null));
    }

    @Test
    void givenMockPayment_whenFeeCalculated_thenPaymentDataIsNotRead() {
        Payment payment = mock(Payment.class);

        BigDecimal fee = rule.calculateFee(payment);

        assertEquals(new BigDecimal("150"), fee);
        verifyNoInteractions(payment);
    }
}
