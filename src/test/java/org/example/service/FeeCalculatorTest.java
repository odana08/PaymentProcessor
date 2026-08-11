package org.example.service;

import org.example.model.Currency;
import org.example.model.NotificationChannel;
import org.example.model.Payment;
import org.example.model.PaymentType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FeeCalculatorTest {

    @Test
    void givenMatchingRule_whenFeeCalculated_thenReturnsRuleResult() {
        Payment payment = payment("CHEQUE_FEE");
        FeeRule domesticRule = ruleFor("DOMESTIC_FEE");
        FeeRule chequeRule = ruleFor("CHEQUE_FEE");
        when(chequeRule.calculateFee(payment)).thenReturn(new BigDecimal("500"));
        FeeCalculator calculator = new FeeCalculator(List.of(domesticRule, chequeRule));

        BigDecimal fee = calculator.calculateFee(payment);

        assertEquals(new BigDecimal("500"), fee);
        verify(domesticRule, never()).calculateFee(payment);
        verify(chequeRule).calculateFee(payment);
    }

    @Test
    void givenUnsupportedPaymentType_whenFeeCalculated_thenThrowsException() {
        Payment payment = payment("UNKNOWN");
        FeeCalculator calculator = new FeeCalculator(List.of(ruleFor("DOMESTIC_FEE")));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> calculator.calculateFee(payment)
        );

        assertEquals("Unsupported payment type: UNKNOWN", exception.getMessage());
    }

    private Payment payment(String type) {
        return new Payment(
                new PaymentType(type),
                new BigDecimal("10000"),
                Currency.JOD,
                new NotificationChannel("EMAIL")
        );
    }

    private FeeRule ruleFor(String type) {
        FeeRule rule = mock(FeeRule.class);
        when(rule.supportedType()).thenReturn(new PaymentType(type));
        return rule;
    }
}
