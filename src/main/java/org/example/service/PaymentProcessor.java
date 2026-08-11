package org.example.service;

import org.example.model.NotificationChannel;
import org.example.model.Payment;
import org.example.model.PaymentRecord;
import org.example.repo.PaymentRepository;

import java.math.BigDecimal;
import java.util.List;

public class PaymentProcessor {

    private final FeeCalculator feeCalculator;
    private final List<NotificationSender> notificationSenders;
    private final PaymentRepository paymentRepository;

    public PaymentProcessor(List<FeeRule> feeRules, List<NotificationSender> notificationSenders,
                            PaymentRepository paymentRepository) {

        this(new FeeCalculator(feeRules), notificationSenders, paymentRepository);
    }

    public PaymentProcessor(FeeCalculator feeCalculator, List<NotificationSender> notificationSenders,
                            PaymentRepository paymentRepository) {

        this.feeCalculator = feeCalculator;
        this.notificationSenders = notificationSenders;
        this.paymentRepository = paymentRepository;
    }

    public PaymentRecord process(Payment payment) {

        BigDecimal fee = feeCalculator.calculateFee(payment);

        PaymentRecord paymentRecord = new PaymentRecord(payment, fee);

        paymentRepository.save(paymentRecord);

        NotificationSender sender = findNotificationSender(payment.getNotificationChannel());

        sender.send(paymentRecord);

        payment.markAsProcessed();
        paymentRepository.update(paymentRecord);

        return paymentRecord;
    }

    private NotificationSender findNotificationSender(NotificationChannel channel) {

        for (NotificationSender sender : notificationSenders) {

            if (sender.supportedChannel().getName().equals(channel.getName())) {
                return sender;
            }
        }

        throw new IllegalArgumentException("Unsupported notification channel: " + channel.getName());
    }
}
