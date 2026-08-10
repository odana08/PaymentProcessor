package org.example;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

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
        when(payment.getAmountInCents()).thenReturn(2_500L);
        when(record.getFeeInCents()).thenReturn(150L);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream originalOutput = System.out;

        try {
            System.setOut(new PrintStream(output));
            sender.send(record);
        } finally {
            System.setOut(originalOutput);
        }

        assertEquals(
                "Email sent. Payment amount: 2500 cents, Fee: 150 cents" + System.lineSeparator(),
                output.toString()
        );
    }
}
