package org.example.service;

import org.example.model.NotificationChannel;
import org.example.model.Currency;
import org.example.model.Payment;
import org.example.model.PaymentStatus;
import org.example.model.PaymentType;
import org.example.repo.PaymentRepository;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentProcessorTest {

    @Test
    void givenMatchingRuleAndSender_whenPaymentProcessed_thenCalculatesLogsAndNotifies() {
        Payment payment = payment("DOMESTIC_FEE", "EMAIL");
        FeeRule rule = mock(FeeRule.class);
        NotificationSender sender = mock(NotificationSender.class);
        PaymentRepository repository = mock(PaymentRepository.class);
        when(rule.supportedType()).thenReturn(new PaymentType("DOMESTIC_FEE"));
        when(rule.calculateFee(payment)).thenReturn(new BigDecimal("150"));
        when(sender.supportedChannel()).thenReturn(new NotificationChannel("EMAIL"));
        PaymentProcessor processor = new PaymentProcessor(List.of(rule), List.of(sender), repository);

        Payment result = processor.process(payment);

        assertSame(payment, result);
        assertEquals(new BigDecimal("150"), result.getFeeInCents());
        assertEquals(PaymentStatus.PROCESSED, payment.getStatus());
        verify(repository).save(result);
        verify(repository).update(result);
        verify(sender).send(result);
    }

    @Test
    void givenMultipleRulesAndSenders_whenPaymentProcessed_thenUsesMatchingCollaborators() {
        Payment payment = payment("CHEQUE_FEE", "SMS");
        FeeRule domesticRule = feeRuleFor("DOMESTIC_FEE");
        FeeRule chequeRule = feeRuleFor("CHEQUE_FEE");
        NotificationSender emailSender = senderFor("EMAIL");
        NotificationSender smsSender = senderFor("SMS");
        PaymentRepository repository = mock(PaymentRepository.class);
        when(chequeRule.calculateFee(payment)).thenReturn(new BigDecimal("500"));
        PaymentProcessor processor = new PaymentProcessor(
                List.of(domesticRule, chequeRule),
                List.of(emailSender, smsSender),
                repository
        );

        Payment result = processor.process(payment);

        assertEquals(new BigDecimal("500"), result.getFeeInCents());
        verify(domesticRule, never()).calculateFee(payment);
        verify(chequeRule).calculateFee(payment);
        verify(emailSender, never()).send(result);
        verify(smsSender).send(result);
    }

    @Test
    void givenUnsupportedPaymentType_whenPaymentProcessed_thenThrowsException() {
        Payment payment = payment("UNKNOWN", "EMAIL");
        FeeRule rule = feeRuleFor("DOMESTIC_FEE");
        PaymentRepository repository = mock(PaymentRepository.class);
        PaymentProcessor processor = new PaymentProcessor(List.of(rule), List.of(), repository);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> processor.process(payment)
        );

        assertEquals("Unsupported payment type: UNKNOWN", exception.getMessage());
        verify(repository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void givenUnsupportedNotificationChannel_whenPaymentProcessed_thenLogsBeforeThrowingException() {
        Payment payment = payment("DOMESTIC_FEE", "PUSH");
        FeeRule rule = feeRuleFor("DOMESTIC_FEE");
        PaymentRepository repository = mock(PaymentRepository.class);
        when(rule.calculateFee(payment)).thenReturn(new BigDecimal("150"));
        PaymentProcessor processor = new PaymentProcessor(List.of(rule), List.of(), repository);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> processor.process(payment));

        assertEquals("Unsupported notification channel: PUSH", exception.getMessage());
        assertEquals(PaymentStatus.CREATED, payment.getStatus());
        verify(repository, never()).update(org.mockito.ArgumentMatchers.any());
        verify(repository).save(org.mockito.ArgumentMatchers.any(Payment.class));
    }

    @Test
    void givenNotificationFailure_whenPaymentProcessed_thenStatusRemainsCreated() {
        Payment payment = payment("DOMESTIC_FEE", "EMAIL");
        FeeRule rule = feeRuleFor("DOMESTIC_FEE");
        NotificationSender sender = senderFor("EMAIL");
        PaymentRepository repository = mock(PaymentRepository.class);
        when(rule.calculateFee(payment)).thenReturn(new BigDecimal("150"));
        doThrow(new IllegalStateException("Notification failed"))
                .when(sender).send(org.mockito.ArgumentMatchers.any(Payment.class));
        PaymentProcessor processor = new PaymentProcessor(List.of(rule), List.of(sender), repository);

        assertThrows(IllegalStateException.class, () -> processor.process(payment));

        assertEquals(PaymentStatus.CREATED, payment.getStatus());
        verify(repository, never()).update(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void givenMultipleNotificationChannels_whenPaymentProcessed_thenSendsThroughAllSelectedChannels() {
        Payment payment = new Payment(
                new PaymentType("DOMESTIC_FEE"),
                new BigDecimal("10000"),
                Currency.JOD,
                List.of(new NotificationChannel("EMAIL"), new NotificationChannel("SMS"))
        );
        FeeRule rule = feeRuleFor("DOMESTIC_FEE");
        NotificationSender emailSender = senderFor("EMAIL");
        NotificationSender smsSender = senderFor("SMS");
        PaymentRepository repository = mock(PaymentRepository.class);
        when(rule.calculateFee(payment)).thenReturn(new BigDecimal("150"));
        PaymentProcessor processor = new PaymentProcessor(
                List.of(rule), List.of(emailSender, smsSender), repository
        );

        Payment result = processor.process(payment);

        verify(emailSender).send(result);
        verify(smsSender).send(result);
        assertEquals(PaymentStatus.PROCESSED, payment.getStatus());
    }

    private Payment payment(String type, String channel) {
        return new Payment(
                new PaymentType(type),
                new BigDecimal("10000"),
                Currency.JOD,
                new NotificationChannel(channel)
        );
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
