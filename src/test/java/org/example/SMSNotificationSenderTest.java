package org.example;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

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
    void givenPaymentRecord_whenSmsSent_thenWritesPaymentDetails() {
        Payment payment = mock(Payment.class);
        PaymentRecord record = mock(PaymentRecord.class);
        when(record.getPayment()).thenReturn(payment);
        when(payment.getAmountInCents()).thenReturn(7_500L);
        when(record.getFeeInCents()).thenReturn(500L);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream originalOutput = System.out;

        try {
            System.setOut(new PrintStream(output));
            sender.send(record);
        } finally {
            System.setOut(originalOutput);
        }

        assertEquals("SMS sent. Payment amount: 7500 cents, Fee: 500 cents" + System.lineSeparator(), output.toString());
    }
}
