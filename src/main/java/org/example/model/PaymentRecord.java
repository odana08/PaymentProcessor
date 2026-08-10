package org.example.model;

import java.math.BigDecimal;

public class PaymentRecord {

    private final Payment payment;
    private final BigDecimal feeInCents;


    public PaymentRecord(Payment payment, BigDecimal feeInCents) {
        this.payment = payment;
        this. feeInCents = feeInCents;
    }

    public Payment getPayment() {
        return payment;
    }

    public BigDecimal getFeeInCents() {
        return feeInCents;
    }

    public BigDecimal getTotalInCents() {
        return payment.getAmountInCents().add(feeInCents);
    }
}
