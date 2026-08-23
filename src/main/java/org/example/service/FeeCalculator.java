package org.example.service;

import org.example.model.Payment;
import org.example.model.PaymentType;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class FeeCalculator {

    private final List<FeeRule> feeRules;

    public FeeCalculator(List<FeeRule> feeRules) {
        this.feeRules = List.copyOf(feeRules);
    }

    public BigDecimal calculateFee(Payment payment) {
        for (FeeRule feeRule : feeRules) {
            if (feeRule.supportedType().getName().equals(payment.getType().getName())) {
                return feeRule.calculateFee(payment);
            }
        }

        throw new IllegalArgumentException("Unsupported payment type: " + payment.getType().getName());
    }

    public List<PaymentType> getAvailableTypes() {
        return feeRules.stream()
                .map(FeeRule::supportedType)
                .toList();
    }
}
