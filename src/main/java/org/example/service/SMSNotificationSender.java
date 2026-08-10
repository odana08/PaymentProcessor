package org.example.service;

import org.example.model.NotificationChannel;
import org.example.model.PaymentRecord;

public class SMSNotificationSender implements NotificationSender {

    @Override
    public NotificationChannel supportedChannel() {
        return new NotificationChannel("SMS");
    }

    @Override
    public void send(PaymentRecord paymentRecord) {
        System.out.println("SMS sent. Payment reference: " + paymentRecord.getPayment().getReference()
                + ", Amount: " + paymentRecord.getPayment().getAmountInCents()
                + " cents, Currency: " + paymentRecord.getPayment().getCurrency()
                + ", Fee: " + paymentRecord.getFeeInCents() + " cents");
    }
}
