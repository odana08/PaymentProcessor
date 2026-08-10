package org.example.service;

import org.example.model.Payment;
import org.example.model.PaymentType;

import java.math.BigDecimal;

public class DomesticFeeRule implements FeeRule {

    private static final BigDecimal FIXED_FEE = new BigDecimal("150");

    @Override
    public PaymentType supportedType() {
        return new PaymentType("DOMESTIC_FEE");
    }

    @Override
    public BigDecimal calculateFee(Payment payment) {
        return FIXED_FEE;
    }
}
