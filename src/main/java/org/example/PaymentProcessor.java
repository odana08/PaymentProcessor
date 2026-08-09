package org.example;

import java.util.List;

public class PaymentProcessor {

    private final List<FeeRule> feeRules;
    private final List<NotificationSender> notificationSenders;
    private final PaymentLog paymentLog;

    public PaymentProcessor(List<FeeRule> feeRules, List<NotificationSender> notificationSenders, PaymentLog paymentLog) {

        this.feeRules = feeRules;
        this.notificationSenders = notificationSenders;
        this.paymentLog = paymentLog;
    }

    public PaymentRecord process(Payment payment) {

        FeeRule feeRule = findFeeRule(payment.getType());

        long fee = feeRule.calculateFee(payment);

        PaymentRecord paymentRecord = new PaymentRecord(payment, fee);

        paymentLog.save(paymentRecord);

        NotificationSender sender = findNotificationSender(payment.getNotificationChannel());

        sender.send(paymentRecord);

        return paymentRecord;
    }

    private FeeRule findFeeRule(PaymentType paymentType) {

        for (FeeRule rule : feeRules) {

            if (rule.supportedType()
                    .getName()
                    .equals(paymentType.getName())) {

                return rule;
            }
        }

        throw new IllegalArgumentException(
                "Unsupported payment type: "
                        + paymentType.getName()
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