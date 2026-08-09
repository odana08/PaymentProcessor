package org.example;

public interface FeeRule {

    PaymentType supportedType();

    long calculateFee(Payment payment);

}
