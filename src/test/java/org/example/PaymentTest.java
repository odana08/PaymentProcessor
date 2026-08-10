package org.example;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class PaymentTest {

    @Test
    void givenPaymentDetails_whenPaymentCreated_thenExposesAllDetails() {
        PaymentType type = new PaymentType("DOMESTIC_FEE");
        NotificationChannel channel = new NotificationChannel("EMAIL");

        Payment payment = new Payment(type, 4_500, channel);

        assertSame(type, payment.getType());
        assertEquals(4_500, payment.getAmountInCents());
        assertSame(channel, payment.getNotificationChannel());
    }
}
