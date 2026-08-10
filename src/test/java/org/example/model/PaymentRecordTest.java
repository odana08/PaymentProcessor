package org.example.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PaymentRecordTest {

    @Test
    void givenPaymentAndFee_whenRecordCreated_thenExposesBothValues() {
        Payment payment = mock(Payment.class);

        PaymentRecord record = new PaymentRecord(payment, new BigDecimal("150"));

        assertSame(payment, record.getPayment());
        assertEquals(new BigDecimal("150"), record.getFeeInCents());
    }

    @Test
    void givenPaymentAndFee_whenTotalRequested_thenAddsFeeToAmount() {
        Payment payment = mock(Payment.class);
        when(payment.getAmountInCents()).thenReturn(new BigDecimal("2000"));
        PaymentRecord record = new PaymentRecord(payment, new BigDecimal("150"));

        assertEquals(new BigDecimal("2150"), record.getTotalInCents());
    }
}
