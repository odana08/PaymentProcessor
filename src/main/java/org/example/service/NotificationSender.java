package org.example.service;

import org.example.model.NotificationChannel;
import org.example.model.PaymentRecord;

public interface NotificationSender {

    NotificationChannel supportedChannel();
    void send(PaymentRecord pr);

}
