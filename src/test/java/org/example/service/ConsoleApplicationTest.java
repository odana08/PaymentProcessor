package org.example.service;

import org.example.repo.PaymentLog;
import org.example.repo.PaymentRepository;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConsoleApplicationTest {

    @Test
    void givenValidInput_whenPaymentProcessed_thenStoresAndDisplaysPayment() {
        PaymentRepository repository = new PaymentLog();
        PaymentProcessor processor = new PaymentProcessor(
                List.of(new DomesticFeeRule()),
                List.of(new EmailNotificationSender()),
                repository
        );
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ConsoleApplication application = new ConsoleApplication(
                new Scanner("1\nDOMESTIC_FEE\n1000\nJOD\nEMAIL\n2\n0\n"),
                new PrintStream(output),
                processor,
                repository
        );

        application.run();

        assertEquals(1, repository.findAll().size());
        assertTrue(output.toString().contains("Payment processed successfully."));
        assertTrue(output.toString().contains("Currency: JOD"));
        assertTrue(output.toString().contains("Status: PROCESSED"));
    }

    @Test
    void givenInvalidAmount_whenPaymentEntered_thenDisplaysValidationError() {
        PaymentRepository repository = new PaymentLog();
        PaymentProcessor processor = new PaymentProcessor(List.of(), List.of(), repository);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ConsoleApplication application = new ConsoleApplication(
                new Scanner("1\nDOMESTIC_FEE\n0\n0\n"),
                new PrintStream(output),
                processor,
                repository
        );

        application.run();

        assertTrue(repository.findAll().isEmpty());
        assertTrue(output.toString().contains("Amount must be greater than zero"));
    }
}
