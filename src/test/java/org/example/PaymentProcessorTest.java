package org.example;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentProcessorTest {

    @Test
    void givenMatchingRuleAndSender_whenPaymentProcessed_thenCalculatesLogsAndNotifies() {
        Payment payment = payment("DOMESTIC_FEE", "EMAIL");
        FeeRule rule = mock(FeeRule.class);
        NotificationSender sender = mock(NotificationSender.class);
        PaymentLog log = mock(PaymentLog.class);
        when(rule.supportedType()).thenReturn(new PaymentType("DOMESTIC_FEE"));
        when(rule.calculateFee(payment)).thenReturn(150L);
        when(sender.supportedChannel()).thenReturn(new NotificationChannel("EMAIL"));
        PaymentProcessor processor = new PaymentProcessor(List.of(rule), List.of(sender), log);

        PaymentRecord result = processor.process(payment);

        assertSame(payment, result.getPayment());
        assertEquals(150, result.getFeeInCents());
        verify(log).save(result);
        verify(sender).send(result);
    }

    @Test
    void givenMultipleRulesAndSenders_whenPaymentProcessed_thenUsesMatchingCollaborators() {
        Payment payment = payment("CHEQUE_FEE", "SMS");
        FeeRule domesticRule = feeRuleFor("DOMESTIC_FEE");
        FeeRule chequeRule = feeRuleFor("CHEQUE_FEE");
        NotificationSender emailSender = senderFor("EMAIL");
        NotificationSender smsSender = senderFor("SMS");
        PaymentLog log = mock(PaymentLog.class);
        when(chequeRule.calculateFee(payment)).thenReturn(500L);
        PaymentProcessor processor = new PaymentProcessor(
                List.of(domesticRule, chequeRule),
                List.of(emailSender, smsSender),
                log
        );

        PaymentRecord result = processor.process(payment);

        assertEquals(500, result.getFeeInCents());
        verify(domesticRule, never()).calculateFee(payment);
        verify(chequeRule).calculateFee(payment);
        verify(emailSender, never()).send(result);
        verify(smsSender).send(result);
    }

    @Test
    void givenUnsupportedPaymentType_whenPaymentProcessed_thenThrowsException() {
        Payment payment = payment("UNKNOWN", "EMAIL");
        FeeRule rule = feeRuleFor("DOMESTIC_FEE");
        PaymentLog log = mock(PaymentLog.class);
        PaymentProcessor processor = new PaymentProcessor(List.of(rule), List.of(), log);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> processor.process(payment)
        );

        assertEquals("Unsupported payment type: UNKNOWN", exception.getMessage());
        verify(log, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void givenUnsupportedNotificationChannel_whenPaymentProcessed_thenLogsBeforeThrowingException() {
        Payment payment = payment("DOMESTIC_FEE", "PUSH");
        FeeRule rule = feeRuleFor("DOMESTIC_FEE");
        PaymentLog log = mock(PaymentLog.class);
        when(rule.calculateFee(payment)).thenReturn(150L);
        PaymentProcessor processor = new PaymentProcessor(List.of(rule), List.of(), log);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> processor.process(payment));

        assertEquals("Unsupported notification channel: PUSH", exception.getMessage());
        verify(log).save(org.mockito.ArgumentMatchers.any(PaymentRecord.class));
    }

    private Payment payment(String type, String channel) {
        return new Payment(new PaymentType(type), 10_000, new NotificationChannel(channel));
    }

    private FeeRule feeRuleFor(String type) {
        FeeRule rule = mock(FeeRule.class);
        when(rule.supportedType()).thenReturn(new PaymentType(type));
        return rule;
    }

    private NotificationSender senderFor(String channel) {
        NotificationSender sender = mock(NotificationSender.class);
        when(sender.supportedChannel()).thenReturn(new NotificationChannel(channel));
        return sender;
    }
}
