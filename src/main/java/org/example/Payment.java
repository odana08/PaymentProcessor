package org.example;

public class Payment {

    private PaymentType type;
    private long amountinCents;
    private final NotificationChannel notificationChannel;


    public Payment (PaymentType type, long amountinCents, NotificationChannel notificationChannel) {
        this.type = type;
        this.amountinCents = amountinCents;
        this.notificationChannel = notificationChannel;
    }

    public PaymentType getType() {
        return type;
    }

    public long getAmountInCents() {
        return amountinCents;
    }

    public NotificationChannel getNotificationChannel () {
        return notificationChannel;
    }
}

