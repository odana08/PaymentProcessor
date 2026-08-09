package org.example;

public class DomesticFeeRule implements FeeRule {

    private static long FIXED_FEE = 150;

    @Override
    public PaymentType supportedType() {
        return new PaymentType("DOMESTIC_FEE");
    }

    @Override
    public long calculateFee(Payment payment) {
        return FIXED_FEE;
    }
}
