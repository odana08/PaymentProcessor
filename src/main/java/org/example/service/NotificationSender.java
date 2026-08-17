package org.example.service;

import org.example.model.NotificationChannel;
import org.example.model.Payment;

public interface NotificationSender {

    NotificationChannel supportedChannel();
    void send(Payment payment);

}
