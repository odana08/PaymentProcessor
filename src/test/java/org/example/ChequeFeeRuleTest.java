package org.example;

import org.junit.jupiter.api.Test;

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
        Payment payment = paymentWithAmount(10_000);

        assertEquals(200, rule.calculateFee(payment));
    }

    @Test
    void givenAmountJustAboveFirstTier_whenFeeCalculated_thenReturnsFiveHundred() {
        Payment payment = paymentWithAmount(10_001);

        assertEquals(500, rule.calculateFee(payment));
    }

    @Test
    void givenAmountAtSecondTierLimit_whenFeeCalculated_thenReturnsFiveHundred() {
        Payment payment = paymentWithAmount(50_000);

        assertEquals(500, rule.calculateFee(payment));
    }

    @Test
    void givenAmountAboveSecondTier_whenFeeCalculated_thenReturnsOneThousand() {
        Payment payment = paymentWithAmount(50_001);

        assertEquals(1_000, rule.calculateFee(payment));
    }

    private Payment paymentWithAmount(long amountInCents) {
        Payment payment = mock(Payment.class);
        when(payment.getAmountInCents()).thenReturn(amountInCents);
        return payment;
    }
}
