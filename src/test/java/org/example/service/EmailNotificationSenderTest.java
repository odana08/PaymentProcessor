package org.example.service;

import org.example.model.Currency;
import org.example.model.Payment;
import org.example.model.PaymentRecord;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EmailNotificationSenderTest {

    private final EmailNotificationSender sender = new EmailNotificationSender();

    @Test
    void givenEmailSender_whenSupportedChannelRequested_thenReturnsEmail() {
        assertEquals("EMAIL", sender.supportedChannel().getName());
    }

    @Test
    void givenPaymentRecord_whenEmailSent_thenWritesPaymentDetails() {
        Payment payment = mock(Payment.class);
        PaymentRecord record = mock(PaymentRecord.class);
        when(record.getPayment()).thenReturn(payment);
        when(payment.getAmountInCents()).thenReturn(new BigDecimal("2500"));
        when(payment.getCurrency()).thenReturn(Currency.JOD);
        UUID reference = UUID.fromString("8a2dd71f-ccaa-4c49-82ea-17222b6ecfee");
        when(payment.getReference()).thenReturn(reference);
        when(record.getFeeInCents()).thenReturn(new BigDecimal("150"));
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream originalOutput = System.out;

        try {
            System.setOut(new PrintStream(output));
            sender.send(record);
        } finally {
            System.setOut(originalOutput);
        }

        assertEquals(
                "Email sent. Payment reference: " + reference
                        + ", Amount: 2500 cents, Currency: JOD, Fee: 150 cents"
                        + System.lineSeparator(),
                output.toString()
        );
    }
}
