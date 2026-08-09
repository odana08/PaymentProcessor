package org.example;

public class InternationalFeeRule implements FeeRule {

    private static final long PERCENTAGE = 2;

    @Override
    public PaymentType supportedType() {
        return new PaymentType("INTERNATIONAL_FEE");
    }

    @Override
    public long calculateFee(Payment p) {
        return p.getAmountInCents() * PERCENTAGE / 100;
    }


}
