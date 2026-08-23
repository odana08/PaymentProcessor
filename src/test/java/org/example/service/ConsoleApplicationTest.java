package org.example.service;

import org.example.model.Currency;
import org.example.model.NotificationChannel;
import org.example.model.Payment;
import org.example.model.PaymentType;
import org.example.repo.PaymentRepository;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConsoleApplicationTest {

    @Test
    void givenValidInput_whenPaymentProcessed_thenStoresAndDisplaysPayment() {
        RepositoryFixture fixture = repositoryFixture();
        PaymentProcessor processor = processor(
                fixture.repository(),
                List.of(new DomesticFeeRule()),
                "EMAIL"
        );
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ConsoleApplication application = application(
                "1\n1\n1000\n1\n1\n2\n0\n",
                output,
                processor,
                fixture.repository()
        );

        application.run();

        assertEquals(1, fixture.payments().size());
        assertTrue(output.toString().contains("Payment processed successfully."));
        assertTrue(output.toString().contains("Currency: JOD"));
        assertTrue(output.toString().contains("Status: PROCESSED"));
    }

    @Test
    void givenInvalidAmount_whenPaymentEntered_thenRetriesBeforeContinuing() {
        RepositoryFixture fixture = repositoryFixture();
        PaymentProcessor processor = processor(
                fixture.repository(),
                List.of(new DomesticFeeRule()),
                "EMAIL"
        );
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ConsoleApplication application = application(
                "1\n1\n0\n1000\n1\n1\n0\n",
                output,
                processor,
                fixture.repository()
        );

        application.run();

        assertEquals(1, fixture.payments().size());
        assertTrue(output.toString().contains("Invalid amount. Enter a number greater than zero."));
        assertTrue(output.toString().contains("Payment processed successfully."));
    }

    @Test
    void givenUnavailableCurrency_whenPaymentEntered_thenShowsFriendlyMessageAndRetries() {
        RepositoryFixture fixture = repositoryFixture();
        PaymentProcessor processor = processor(
                fixture.repository(),
                List.of(new DomesticFeeRule()),
                "EMAIL"
        );
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ConsoleApplication application = application(
                "1\n1\n1000\n99\n1\n1\n0\n",
                output,
                processor,
                fixture.repository()
        );

        application.run();

        assertEquals(1, fixture.payments().size());
        assertTrue(output.toString().contains("Currency not available."));
        assertTrue(output.toString().contains("1. Transfer payment"));
    }

    @Test
    void givenRegisteredRules_whenMenuOpened_thenDisplaysPaymentTypesDynamically() {
        RepositoryFixture fixture = repositoryFixture();
        PaymentProcessor processor = processor(
                fixture.repository(),
                List.of(new DomesticFeeRule(), new ChequeFeeRule())
        );
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ConsoleApplication application = application(
                "1\n",
                output,
                processor,
                fixture.repository()
        );

        application.run();

        assertTrue(output.toString().contains("1. DOMESTIC"));
        assertTrue(output.toString().contains("2. CHEQUE"));
        assertFalse(output.toString().contains("DOMESTIC_FEE"));
    }

    @Test
    void givenCopiedConsoleLabels_whenPastedAsInput_thenProcessesPayment() {
        RepositoryFixture fixture = repositoryFixture();
        PaymentProcessor processor = processor(
                fixture.repository(),
                List.of(new DomesticFeeRule()),
                "EMAIL",
                "SMS"
        );
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ConsoleApplication application = application(
                "1. Transfer payment\n1. DOMESTIC\n1000\n1. JOD\n1. EMAIL, 2. SMS\n0. Exit\n",
                output,
                processor,
                fixture.repository()
        );

        application.run();

        assertEquals(1, fixture.payments().size());
        assertTrue(output.toString().contains("Payment processed successfully."));
        assertFalse(output.toString().contains("Exception"));
    }

    @Test
    void givenInvalidReference_whenDeletingPayment_thenRetriesUntilReferenceIsValid() {
        RepositoryFixture fixture = repositoryFixture();
        Payment payment = new Payment(
                new PaymentType("DOMESTIC_FEE"),
                new BigDecimal("1000"),
                Currency.JOD,
                new NotificationChannel("EMAIL")
        );
        payment.setFeeInCents(new BigDecimal("150"));
        fixture.payments().add(payment);
        PaymentProcessor processor = processor(fixture.repository(), List.of());
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ConsoleApplication application = application(
                "4\nnot-a-reference\n" + payment.getReference() + "\n2\n0\n",
                output,
                processor,
                fixture.repository()
        );

        application.run();

        assertTrue(fixture.payments().isEmpty());
        assertTrue(output.toString().contains("Invalid payment reference. Please try again."));
        assertTrue(output.toString().contains("Payment deleted."));
        assertTrue(output.toString().contains("No payments found."));
    }

    private ConsoleApplication application(String input, ByteArrayOutputStream output,
                                           PaymentProcessor processor, PaymentRepository repository) {
        return new ConsoleApplication(
                new Scanner(input),
                new PrintStream(output),
                processor,
                repository
        );
    }

    private PaymentProcessor processor(PaymentRepository repository, List<FeeRule> rules,
                                       String... channelNames) {
        List<NotificationSender> senders = java.util.Arrays.stream(channelNames)
                .map(this::sender)
                .toList();
        return new PaymentProcessor(new FeeCalculator(rules), senders, repository);
    }

    private NotificationSender sender(String channelName) {
        NotificationSender sender = mock(NotificationSender.class);
        when(sender.supportedChannel()).thenReturn(new NotificationChannel(channelName));
        return sender;
    }

    private RepositoryFixture repositoryFixture() {
        PaymentRepository repository = mock(PaymentRepository.class);
        List<Payment> payments = new ArrayList<>();

        when(repository.saveAndFlush(any(Payment.class))).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            payments.add(payment);
            return payment;
        });
        when(repository.findAll()).thenAnswer(invocation -> List.copyOf(payments));
        when(repository.findByReference(any(UUID.class))).thenAnswer(invocation -> {
            UUID reference = invocation.getArgument(0);
            for (Payment payment : payments) {
                if (payment.getReference().equals(reference)) {
                    return Optional.of(payment);
                }
            }
            return Optional.empty();
        });
        when(repository.deleteByReference(any(UUID.class))).thenAnswer(invocation -> {
            UUID reference = invocation.getArgument(0);
            int previousSize = payments.size();
            for (int index = payments.size() - 1; index >= 0; index--) {
                Payment payment = payments.get(index);
                if (payment.getReference().equals(reference)) {
                    payments.remove(index);
                }
            }
            return (long) (previousSize - payments.size());
        });

        return new RepositoryFixture(repository, payments);
    }

    private record RepositoryFixture(PaymentRepository repository, List<Payment> payments) {
    }
}
