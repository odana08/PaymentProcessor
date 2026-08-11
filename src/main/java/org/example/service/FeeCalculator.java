package org.example.service;

import org.example.model.Payment;

import java.math.BigDecimal;
import java.util.List;

public class FeeCalculator {

    private final List<FeeRule> feeRules;

    public FeeCalculator(List<FeeRule> feeRules) {
        this.feeRules = List.copyOf(feeRules);
    }

    public BigDecimal calculateFee(Payment payment) {
        for (FeeRule rule : feeRules) {
            if (rule.supportedType().getName().equals(payment.getType().getName())) {
                return rule.calculateFee(payment);
            }
        }

        throw new IllegalArgumentException("Unsupported payment type: " + payment.getType().getName());
    }
}
