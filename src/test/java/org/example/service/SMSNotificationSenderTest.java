package org.example.service;

import org.example.model.Currency;
import org.example.model.Payment;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SMSNotificationSenderTest {

    private final SMSNotificationSender sender = new SMSNotificationSender();

    @Test
    void givenSmsSender_whenSupportedChannelRequested_thenReturnsSms() {
        assertEquals("SMS", sender.supportedChannel().getName());
    }

    @Test
    void givenPayment_whenSmsSent_thenWritesPaymentDetails() {
        Payment payment = mock(Payment.class);
        when(payment.getAmountInCents()).thenReturn(new BigDecimal("7500"));
        when(payment.getCurrency()).thenReturn(Currency.USD);
        UUID reference = UUID.fromString("4fc58cd6-c031-4d6e-9dd7-c03280d15e3f");
        when(payment.getReference()).thenReturn(reference);
        when(payment.getFeeInCents()).thenReturn(new BigDecimal("500"));
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream originalOutput = System.out;

        try {
            System.setOut(new PrintStream(output));
            sender.send(payment);
        } finally {
            System.setOut(originalOutput);
        }

        assertEquals(
                "SMS sent. Payment reference: " + reference
                        + ", Amount: 7500 cents, Currency: USD, Fee: 500 cents"
                        + System.lineSeparator(),
                output.toString()
        );
    }
}
