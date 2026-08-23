package org.example.service;

import org.example.model.NotificationChannel;
import org.example.model.Payment;
import org.springframework.stereotype.Component;

@Component
public class EmailNotificationSender implements NotificationSender {
    @Override
    public NotificationChannel supportedChannel() {
        return new NotificationChannel("EMAIL");
    }

    @Override
    public void send(Payment payment) {
        System.out.println("Email sent. Payment reference: " + payment.getReference()
                + ", Amount: " + payment.getAmountInCents()
                + " cents, Currency: " + payment.getCurrency()
                + ", Fee: " + payment.getFeeInCents() + " cents");
    }
}
