package org.example.service;

import org.example.model.NotificationChannel;
import org.example.model.Payment;
import org.example.repo.PaymentRepository;

import java.math.BigDecimal;
import java.util.ArrayList;
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
        this.notificationSenders = List.copyOf(notificationSenders);
        this.paymentRepository = paymentRepository;
    }

    public Payment process(Payment payment) {

        BigDecimal fee = feeCalculator.calculateFee(payment);

        payment.setFeeInCents(fee);

        paymentRepository.save(payment);

        for (NotificationChannel channel : payment.getNotificationChannels()) {
            findNotificationSender(channel).send(payment);
        }

        payment.markAsProcessed();
        paymentRepository.update(payment);

        return payment;
    }

    public List<org.example.model.PaymentType> getAvailablePaymentTypes() {
        return feeCalculator.getAvailableTypes();
    }

    public List<NotificationChannel> getAvailableNotificationChannels() {
        List<NotificationChannel> channels = new ArrayList<>();
        for (NotificationSender sender : notificationSenders) {
            channels.add(sender.supportedChannel());
        }
        return List.copyOf(channels);
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
