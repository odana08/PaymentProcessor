package org.example;

public class EmailNotificationSender implements NotificationSender {
    @Override
    public NotificationChannel supportedChannel() {
        return new NotificationChannel("EMAIL");
    }

    @Override
    public void send(PaymentRecord paymentRecord) {
        System.out.println("Email sent. Payment amount: " + paymentRecord.getPayment().getAmountInCents() + " cents, Fee: " + paymentRecord.getFeeInCents() + " cents");
    }
}
