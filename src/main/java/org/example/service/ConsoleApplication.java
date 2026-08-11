package org.example.service;

import org.example.model.Currency;
import org.example.model.NotificationChannel;
import org.example.model.Payment;
import org.example.model.PaymentRecord;
import org.example.model.PaymentType;
import org.example.repo.PaymentRepository;

import java.io.PrintStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
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
        while (true) {
            printMenu();
            String option = nextLine();
            if (option == null || choiceIndex(option).equals("0")) {
                break;
            }

            try {
                switch (choiceIndex(option)) {
                    case "1" -> processPayment();
                    case "2" -> listPayments();
                    case "3" -> findPayment();
                    case "4" -> deletePayment();
                    default -> output.println("Invalid option. Please select a menu number.");
                }
            } catch (RuntimeException exception) {
                output.println("The operation could not be completed. Please try again.");
            }
        }
        output.println("Goodbye.");
    }

    private void printMenu() {
        output.println();
        output.println("1. Transfer payment");
        output.println("2. List payments");
        output.println("3. Find payment by reference");
        output.println("4. Delete payment by reference");
        output.println("0. Exit");
        output.print("Choose an option: ");
    }

    private void processPayment() {
        List<PaymentType> types = paymentProcessor.getAvailablePaymentTypes();
        if (types.isEmpty()) {
            output.println("No payment types are available.");
            return;
        }

        PaymentType type = chooseOne("Payment type", types);
        if (type == null) return;

        BigDecimal amount = readPositiveAmount();
        if (amount == null) return;

        Currency currency = chooseCurrency();
        if (currency == null) return;

        List<NotificationChannel> channels = chooseChannels();
        if (channels == null) return;

        try {
            PaymentRecord record = paymentProcessor.process(new Payment(type, amount, currency, channels));
            output.println("Payment processed successfully.");
            printRecord(record);
        } catch (RuntimeException exception) {
            output.println("Could not process payment. Please check the entered details and try again.");
        }
    }

    private <T> T chooseOne(String label, List<T> options) {
        while (true) {
            output.println(label + ":");
            for (int i = 0; i < options.size(); i++) {
                output.println((i + 1) + ". " + displayOption(options.get(i)));
            }
            output.print("Select an index: ");
            String value = nextLine();
            if (value == null) return null;
            Integer index = parseIndex(value, options.size());
            if (index != null) return options.get(index);
            output.println(label + " not available. Please select a listed index.");
        }
    }

    private BigDecimal readPositiveAmount() {
        while (true) {
            output.print("Amount in cents: ");
            String value = nextLine();
            if (value == null) return null;
            try {
                BigDecimal amount = new BigDecimal(value);
                if (amount.signum() > 0) return amount;
            } catch (NumberFormatException ignored) {
                // The friendly validation message below is enough for a console user.
            }
            output.println("Invalid amount. Enter a number greater than zero.");
        }
    }

    private Currency chooseCurrency() {
        List<Currency> currencies = Arrays.asList(Currency.values());
        while (true) {
            output.println("Currency:");
            for (int i = 0; i < currencies.size(); i++) {
                output.println((i + 1) + ". " + currencies.get(i));
            }
            output.print("Select an index: ");
            String value = nextLine();
            if (value == null) return null;
            Integer index = parseIndex(value, currencies.size());
            if (index != null) return currencies.get(index);
            output.println("Currency not available. Please select a listed index.");
        }
    }

    private List<NotificationChannel> chooseChannels() {
        List<NotificationChannel> options = paymentProcessor.getAvailableNotificationChannels();
        if (options.isEmpty()) {
            output.println("No notification channels are available.");
            return null;
        }
        while (true) {
            output.println("Notification options (select one or more):");
            for (int i = 0; i < options.size(); i++) {
                output.println((i + 1) + ". " + options.get(i));
            }
            output.print("Enter indexes separated by commas: ");
            String value = nextLine();
            if (value == null) return null;

            Set<Integer> indexes = new LinkedHashSet<>();
            boolean valid = !value.isBlank();
            for (String part : value.split(",")) {
                Integer index = parseIndex(part.trim(), options.size());
                if (index == null) {
                    valid = false;
                    break;
                }
                indexes.add(index);
            }
            if (valid) {
                List<NotificationChannel> selected = new ArrayList<>();
                indexes.forEach(index -> selected.add(options.get(index)));
                return selected;
            }
            output.println("Notification option not available. Please use listed indexes.");
        }
    }

    private Integer parseIndex(String value, int size) {
        try {
            int index = Integer.parseInt(choiceIndex(value)) - 1;
            return index >= 0 && index < size ? index : null;
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private String choiceIndex(String value) {
        String trimmed = value.trim();
        int separator = 0;
        while (separator < trimmed.length() && Character.isDigit(trimmed.charAt(separator))) {
            separator++;
        }
        return trimmed.substring(0, separator);
    }

    private String displayOption(Object option) {
        if (option instanceof PaymentType paymentType) {
            return paymentType.getName().replaceFirst("_FEE$", "");
        }
        return option.toString();
    }

    private String nextLine() {
        return scanner.hasNextLine() ? scanner.nextLine().trim() : null;
    }

    private void listPayments() {
        List<PaymentRecord> records = paymentRepository.findAll();
        if (records.isEmpty()) {
            output.println("No payments found.");
            return;
        }
        records.forEach(this::printRecord);
    }

    private void findPayment() {
        UUID reference = readReference();
        if (reference == null) return;
        paymentRepository.findByReference(reference)
                .ifPresentOrElse(this::printRecord, () -> output.println("Payment not found."));
    }

    private void deletePayment() {
        UUID reference = readReference();
        if (reference == null) return;
        if (paymentRepository.findByReference(reference).isEmpty()) {
            output.println("Payment not found.");
            return;
        }
        paymentRepository.deleteByReference(reference);
        output.println("Payment deleted.");
    }

    private UUID readReference() {
        while (true) {
            output.print("Payment reference: ");
            String value = nextLine();
            if (value == null) return null;
            try {
                return UUID.fromString(value);
            } catch (IllegalArgumentException exception) {
                output.println("Invalid payment reference. Please try again.");
            }
        }
    }

    private void printRecord(PaymentRecord record) {
        Payment payment = record.getPayment();
        output.println("Reference: " + payment.getReference()
                + ", Type: " + displayOption(payment.getType())
                + ", Amount: " + payment.getAmountInCents()
                + ", Currency: " + payment.getCurrency()
                + ", Fee: " + record.getFeeInCents()
                + ", Total: " + record.getTotalInCents()
                + ", Status: " + payment.getStatus());
    }
}
