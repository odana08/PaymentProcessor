package org.example.service;

import org.example.repo.PaymentLog;
import org.example.repo.PaymentRepository;

import java.util.List;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        PaymentRepository paymentRepository = new PaymentLog();
        FeeCalculator feeCalculator = new FeeCalculator(
                new DomesticFeeRuleCreator(),
                new InternationalFeeRuleCreator(),
                new ChequeFeeRuleCreator()
        );
        PaymentProcessor paymentProcessor = new PaymentProcessor(
                feeCalculator,
                List.of(new EmailNotificationSender(), new SMSNotificationSender()),
                paymentRepository
        );

        ConsoleApplication application = new ConsoleApplication(new Scanner(System.in), System.out, paymentProcessor, paymentRepository);
        application.run();
    }
}
