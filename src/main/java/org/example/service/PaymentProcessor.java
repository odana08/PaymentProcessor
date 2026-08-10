package org.example.service;

import org.example.model.NotificationChannel;
import org.example.model.Payment;
import org.example.model.PaymentRecord;
import org.example.model.PaymentType;
import org.example.repo.PaymentRepository;

import java.math.BigDecimal;
import java.util.List;

public class PaymentProcessor {

    private final List<FeeRule> feeRules;
    private final List<NotificationSender> notificationSenders;
    private final PaymentRepository paymentRepository;

    public PaymentProcessor(List<FeeRule> feeRules, List<NotificationSender> notificationSenders,
                            PaymentRepository paymentRepository) {

        this.feeRules = feeRules;
        this.notificationSenders = notificationSenders;
        this.paymentRepository = paymentRepository;
    }

    public PaymentRecord process(Payment payment) {

        FeeRule feeRule = findFeeRule(payment.getType());

        BigDecimal fee = feeRule.calculateFee(payment);

        PaymentRecord paymentRecord = new PaymentRecord(payment, fee);

        paymentRepository.save(paymentRecord);

        NotificationSender sender = findNotificationSender(payment.getNotificationChannel());

        sender.send(paymentRecord);

        payment.markAsProcessed();
        paymentRepository.update(paymentRecord);

        return paymentRecord;
    }

    private FeeRule findFeeRule(PaymentType paymentType) {

        for (FeeRule rule : feeRules) {

            if (rule.supportedType().getName().equals(paymentType.getName())) {
                return rule;
            }
        }

        throw new IllegalArgumentException("Unsupported payment type: " + paymentType.getName()
        );
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
