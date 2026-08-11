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
        this.notificationSenders = List.copyOf(notificationSenders);
        this.paymentRepository = paymentRepository;
    }

    public PaymentRecord process(Payment payment) {

        BigDecimal fee = feeCalculator.calculateFee(payment);

        PaymentRecord paymentRecord = new PaymentRecord(payment, fee);

        paymentRepository.save(paymentRecord);

        for (NotificationChannel channel : payment.getNotificationChannels()) {
            findNotificationSender(channel).send(paymentRecord);
        }

        payment.markAsProcessed();
        paymentRepository.update(paymentRecord);

        return paymentRecord;
    }

    public List<org.example.model.PaymentType> getAvailablePaymentTypes() {
        return feeCalculator.getAvailableTypes();
    }

    public List<NotificationChannel> getAvailableNotificationChannels() {
        return notificationSenders.stream().map(NotificationSender::supportedChannel).toList();
    }

    private NotificationSender findNotificationSender(NotificationChannel channel) {
        return notificationSenders.stream()
                .filter(sender -> sender.supportedChannel().getName().equals(channel.getName()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unsupported notification channel: " + channel.getName()));
    }
}
