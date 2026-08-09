package org.example;

public class PaymentRecord {

    private final Payment payment;
    private final long feeInCents;


    public PaymentRecord(Payment payment, long feeInCents) {
        this.payment = payment;
        this. feeInCents = feeInCents;
    }

    public Payment getPayment() {
        return payment;
    }

    public long getFeeInCents() {
        return feeInCents;
    }

    public long getTotalInCents() {
        return feeInCents * payment.getAmountInCents();
    }
}
