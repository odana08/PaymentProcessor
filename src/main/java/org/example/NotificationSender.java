package org.example;

public interface NotificationSender {

    NotificationChannel supportedChannel();
    void send(PaymentRecord pr);

}
