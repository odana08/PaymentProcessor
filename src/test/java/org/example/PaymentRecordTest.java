package org.example;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PaymentRecordTest {

    @Test
    void givenPaymentAndFee_whenRecordCreated_thenExposesBothValues() {
        Payment payment = mock(Payment.class);

        PaymentRecord record = new PaymentRecord(payment, 150);

        assertSame(payment, record.getPayment());
        assertEquals(150, record.getFeeInCents());
    }

    @Test
    void givenPaymentAndFee_whenTotalRequested_thenMultipliesFeeByAmount() {
        Payment payment = mock(Payment.class);
        when(payment.getAmountInCents()).thenReturn(2_000L);
        PaymentRecord record = new PaymentRecord(payment, 150);

        assertEquals(300_000, record.getTotalInCents());
    }
}
