package org.example.service;

import org.example.model.Payment;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ChequeFeeRuleTest {

    private final ChequeFeeRule rule = new ChequeFeeRule();

    @Test
    void givenChequeFeeRule_whenSupportedTypeRequested_thenReturnsChequeFee() {
        assertEquals("CHEQUE_FEE", rule.supportedType().getName());
    }

    @Test
    void givenAmountAtFirstTierLimit_whenFeeCalculated_thenReturnsTwoHundred() {
        Payment payment = paymentWithAmount("10000");

        assertEquals(new BigDecimal("200"), rule.calculateFee(payment));
    }

    @Test
    void givenAmountJustAboveFirstTier_whenFeeCalculated_thenReturnsFiveHundred() {
        Payment payment = paymentWithAmount("10001");

        assertEquals(new BigDecimal("500"), rule.calculateFee(payment));
    }

    @Test
    void givenAmountAtSecondTierLimit_whenFeeCalculated_thenReturnsFiveHundred() {
        Payment payment = paymentWithAmount("50000");

        assertEquals(new BigDecimal("500"), rule.calculateFee(payment));
    }

    @Test
    void givenAmountAboveSecondTier_whenFeeCalculated_thenReturnsOneThousand() {
        Payment payment = paymentWithAmount("50001");

        assertEquals(new BigDecimal("1000"), rule.calculateFee(payment));
    }

    private Payment paymentWithAmount(String amountInCents) {
        Payment payment = mock(Payment.class);
        when(payment.getAmountInCents()).thenReturn(new BigDecimal(amountInCents));
        return payment;
    }
}
