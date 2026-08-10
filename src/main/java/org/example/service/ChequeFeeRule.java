package org.example.service;

import org.example.model.Payment;
import org.example.model.PaymentType;

import java.math.BigDecimal;

public class ChequeFeeRule implements FeeRule {

    @Override
    public PaymentType supportedType() {
        return new PaymentType("CHEQUE_FEE");
    }

    @Override
    public BigDecimal calculateFee(Payment p) {
        BigDecimal amount = p.getAmountInCents();

        if (amount.compareTo(new BigDecimal("10000")) <= 0) {
            return new BigDecimal("200");
        }

        else if (amount.compareTo(new BigDecimal("50000")) <= 0) {
            return new BigDecimal("500");
        }
        else {
            return new BigDecimal("1000");
        }
    }

}
