package org.example;

public class ChequeFeeRule implements FeeRule {

    @Override
    public PaymentType supportedType() {
        return new PaymentType("CHEQUE_FEE");
    }

    @Override
    public long calculateFee(Payment p) {
        long amount = p.getAmountInCents();

        if (amount <= 10000 ) {
            return 200;
        }

        else if (amount <= 50000) {
            return 500;
        }
        else {
            return 1000;
        }
    }

}
