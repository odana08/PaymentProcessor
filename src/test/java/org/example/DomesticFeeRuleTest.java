package org.example;

import org.junit.jupiter.api.Test;

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
        Payment payment = new Payment(new PaymentType("DOMESTIC_FEE"), 10_000, new NotificationChannel("EMAIL"));

        assertEquals(150, rule.calculateFee(payment));
    }

    @Test
    void givenNullPayment_whenFeeCalculated_thenReturnsFixedFee() {
        assertEquals(150, rule.calculateFee(null));
    }

    @Test
    void givenMockPayment_whenFeeCalculated_thenPaymentDataIsNotRead() {
        Payment payment = mock(Payment.class);

        long fee = rule.calculateFee(payment);

        assertEquals(150, fee);
        verifyNoInteractions(payment);
    }
}
