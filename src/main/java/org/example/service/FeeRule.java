package org.example.service;

import org.example.model.Payment;
import org.example.model.PaymentType;

import java.math.BigDecimal;

public interface FeeRule {

    PaymentType supportedType();

    BigDecimal calculateFee(Payment payment);

}
