package org.example;

public class SMSNotificationSender implements NotificationSender {

    @Override
    public NotificationChannel supportedChannel() {
        return new NotificationChannel("SMS");
    }

    @Override
    public void send(PaymentRecord paymentRecord) {
        System.out.println("SMS sent. Payment amount: " + paymentRecord.getPayment().getAmountInCents() + " cents, Fee: " + paymentRecord.getFeeInCents() + " cents");
    }
}
