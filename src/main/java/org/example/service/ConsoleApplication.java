package org.example.service;

import org.example.model.Currency;
import org.example.model.NotificationChannel;
import org.example.model.Payment;
import org.example.model.PaymentRecord;
import org.example.model.PaymentType;
import org.example.repo.PaymentRepository;

import java.io.PrintStream;
import java.math.BigDecimal;
import java.util.Scanner;
import java.util.UUID;

public class ConsoleApplication {

    private final Scanner scanner;
    private final PrintStream output;
    private final PaymentProcessor paymentProcessor;
    private final PaymentRepository paymentRepository;

    public ConsoleApplication(Scanner scanner, PrintStream output, PaymentProcessor paymentProcessor,
                              PaymentRepository paymentRepository) {
        this.scanner = scanner;
        this.output = output;
        this.paymentProcessor = paymentProcessor;
        this.paymentRepository = paymentRepository;
    }

    public void run() {
        output.println("Fee Calculator");

        boolean running = true;
        while (running && scanner.hasNextLine()) {
            printMenu();
            String option = scanner.nextLine().trim();

            if (option.equals("1")) {
                processPayment();
            } else if (option.equals("2")) {
                listPayments();
            } else if (option.equals("3")) {
                findPayment();
            } else if (option.equals("4")) {
                deletePayment();
            } else if (option.equals("0")) {
                running = false;
            } else {
                output.println("Invalid option. Please try again.");
            }
        }

        output.println("Goodbye.");
    }

    private void printMenu() {
        output.println();
        output.println("1. Process payment");
        output.println("2. List payments");
        output.println("3. Find payment by reference");
        output.println("4. Delete payment by reference");
        output.println("0. Exit");
        output.print("Choose an option: ");
    }

    private void processPayment() {
        try {
            output.print("Payment type (DOMESTIC_FEE, INTERNATIONAL_FEE, CHEQUE_FEE): ");
            PaymentType type = new PaymentType(readRequiredLine());

            output.print("Amount in cents: ");
            BigDecimal amount = new BigDecimal(readRequiredLine());
            if (amount.signum() <= 0) {
                throw new IllegalArgumentException("Amount must be greater than zero");
            }

            output.print("Currency (JOD, USD): ");
            Currency currency = Currency.valueOf(readRequiredLine().toUpperCase());

            output.print("Notification channel (EMAIL, SMS): ");
            NotificationChannel channel = new NotificationChannel(readRequiredLine().toUpperCase());

            Payment payment = new Payment(type, amount, currency, channel);
            PaymentRecord record = paymentProcessor.process(payment);

            output.println("Payment processed successfully.");
            printRecord(record);
        } catch (IllegalArgumentException exception) {
            output.println("Could not process payment: " + exception.getMessage());
        }
    }

    private String readRequiredLine() {
        if (!scanner.hasNextLine()) {
            throw new IllegalArgumentException("Input is required");
        }

        String value = scanner.nextLine().trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("Input is required");
        }
        return value;
    }

    private void listPayments() {
        if (paymentRepository.findAll().isEmpty()) {
            output.println("No payments found.");
            return;
        }

        for (PaymentRecord record : paymentRepository.findAll()) {
            printRecord(record);
        }
    }

    private void findPayment() {
        try {
            output.print("Payment reference: ");
            UUID reference = UUID.fromString(readRequiredLine());
            paymentRepository.findByReference(reference)
                    .ifPresentOrElse(this::printRecord, () -> output.println("Payment not found."));
        } catch (IllegalArgumentException exception) {
            output.println("Invalid payment reference.");
        }
    }

    private void deletePayment() {
        try {
            output.print("Payment reference: ");
            UUID reference = UUID.fromString(readRequiredLine());

            if (paymentRepository.findByReference(reference).isEmpty()) {
                output.println("Payment not found.");
                return;
            }

            paymentRepository.deleteByReference(reference);
            output.println("Payment deleted.");
        } catch (IllegalArgumentException exception) {
            output.println("Invalid payment reference.");
        }
    }

    private void printRecord(PaymentRecord record) {
        Payment payment = record.getPayment();
        output.println("Reference: " + payment.getReference()
                + ", Type: " + payment.getType()
                + ", Amount: " + payment.getAmountInCents()
                + ", Currency: " + payment.getCurrency()
                + ", Fee: " + record.getFeeInCents()
                + ", Total: " + record.getTotalInCents()
                + ", Status: " + payment.getStatus());
    }
}
