package org.example.service;

import org.example.model.Payment;
import org.example.model.PaymentType;

import java.math.BigDecimal;

public class InternationalFeeRule implements FeeRule {

    private static final BigDecimal PERCENTAGE = new BigDecimal("0.02");

    @Override
    public PaymentType supportedType() {
        return new PaymentType("INTERNATIONAL_FEE");
    }

    @Override
    public BigDecimal calculateFee(Payment p) {
        return p.getAmountInCents().multiply(PERCENTAGE);
    }


}
