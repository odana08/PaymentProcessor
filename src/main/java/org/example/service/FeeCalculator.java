package org.example.service;

import org.example.model.Payment;
import org.example.model.PaymentType;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.ArrayList;

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
        List<PaymentType> types = new ArrayList<>();
        for (FeeRule feeRule : feeRules) {
            types.add(feeRule.supportedType());
        }
        return List.copyOf(types);
    }
}
