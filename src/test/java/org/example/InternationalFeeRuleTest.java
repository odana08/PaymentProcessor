package org.example;

import org.junit.jupiter.api.Test;

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
        when(payment.getAmountInCents()).thenReturn(12_500L);

        assertEquals(250, rule.calculateFee(payment));
    }

    @Test
    void givenAmountWithFractionalFee_whenFeeCalculated_thenRoundsDownToWholeCent() {
        Payment payment = mock(Payment.class);
        when(payment.getAmountInCents()).thenReturn(199L);

        assertEquals(3, rule.calculateFee(payment));
    }
}
