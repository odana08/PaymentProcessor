package org.example.service;

import org.example.model.Payment;
import org.example.model.PaymentType;

import java.math.BigDecimal;

public abstract class FeeRuleCreator {

    public BigDecimal calculateFee(Payment payment) {
        return createFeeRule().calculateFee(payment);
    }

    public PaymentType supportedType() {
        return createFeeRule().supportedType();
    }

    protected abstract FeeRule createFeeRule();
}
