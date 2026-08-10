package org.example.service;

import org.example.model.Payment;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InternationalFeeRuleTest {

    private final InternationalFeeRule rule = new InternationalFeeRule();

    @Test
    void givenInternationalFeeRule_whenSupportedTypeRequested_thenReturnsInternationalFee() {
        assertEquals("INTERNATIONAL_FEE", rule.supportedType().getName());
    }

    @Test
    void givenPaymentAmount_whenFeeCalculated_thenReturnsTwoPercent() {
        Payment payment = mock(Payment.class);
        when(payment.getAmountInCents()).thenReturn(new BigDecimal("12500"));

        assertEquals(new BigDecimal("250.00"), rule.calculateFee(payment));
    }

    @Test
    void givenAmountWithFractionalFee_whenFeeCalculated_thenPreservesDecimalPrecision() {
        Payment payment = mock(Payment.class);
        when(payment.getAmountInCents()).thenReturn(new BigDecimal("199"));

        assertEquals(new BigDecimal("3.98"), rule.calculateFee(payment));
    }
}
