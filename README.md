# Fee Calculator

A small Java application that calculates payment fees, records processed payments,
and sends a notification through the payment's selected channel.

The project demonstrates a rule-based design: fee calculations and notification
channels can be extended without changing the central `PaymentProcessor`.

## Console application

Run `org.example.service.Main` from your IDE to open the interactive console.
The menu supports processing a payment, listing all payments, finding a payment
by its UUID reference, and deleting a payment.

## Features

- Calculates fees for domestic, international, and cheque payments
- Assigns a unique UUID reference to every payment
- Supports `JOD` and `USD` currencies
- Tracks each payment from `CREATED` to `PROCESSED`
- Selects fee rules by payment type
- Sends email or SMS notifications
- Stores processed payments in an in-memory log
- Rejects unsupported payment types and notification channels
- Includes unit tests for the domain objects, rules, senders, log, and processor

## Fee rules

All monetary values are represented in cents using `BigDecimal`.

| Payment type | Fee |
| --- | ---: |
| `DOMESTIC_FEE` | 150 cents |
| `INTERNATIONAL_FEE` | 2% of the payment amount |
| `CHEQUE_FEE` up to 10,000 cents | 200 cents |
| `CHEQUE_FEE` from 10,001 to 50,000 cents | 500 cents |
| `CHEQUE_FEE` above 50,000 cents | 1,000 cents |

Supported notification channels are `EMAIL` and `SMS`.

## Usage

Create the available rules and senders, then pass them to a `PaymentProcessor`:

```java
import org.example.model.*;
import org.example.repo.PaymentLog;
import org.example.repo.PaymentRepository;
import org.example.service.*;

import java.math.BigDecimal;
import java.util.List;

PaymentRepository paymentRepository = new PaymentLog();

PaymentProcessor processor = new PaymentProcessor(
    List.of(
        new DomesticFeeRule(),
        new InternationalFeeRule(),
        new ChequeFeeRule()
    ),
    List.of(
        new EmailNotificationSender(),
        new SMSNotificationSender()
    ),
    paymentRepository
);

Payment payment = new Payment(
    new PaymentType("INTERNATIONAL_FEE"),
    new BigDecimal("25000"),
    Currency.JOD,
    new NotificationChannel("EMAIL")
);

PaymentRecord record = processor.process(payment);

System.out.println(record.getFeeInCents()); // 500
System.out.println(payment.getReference()); // unique UUID
System.out.println(payment.getStatus()); // PROCESSED
System.out.println(paymentRepository.findAll().size()); // 1
```

Processing follows this sequence:

1. Find the fee rule matching the payment type.
2. Calculate the fee.
3. Create and save a `PaymentRecord`.
4. Find the selected notification sender and send the notification.
5. Mark the payment as `PROCESSED` after the notification succeeds and persist
   the updated record.
6. Return the payment record.

Important extension points:

- `FeeRule` defines a supported payment type and its fee calculation.
- `NotificationSender` defines a supported channel and how notifications are sent.
- `PaymentRepository` abstracts payment-record persistence. `PaymentLog` is the
  current in-memory implementation; another implementation can be added for
  Spring Boot persistence later.
- `PaymentProcessor` coordinates calculation, persistence, and notification.

## Extending the project

To add a payment type, implement `FeeRule` and include the new instance in the
processor's fee-rule list. To add a notification channel, implement
`NotificationSender` and include it in the sender list.

Names are matched exactly and are case-sensitive, so the value returned by an
implementation's `supportedType()` or `supportedChannel()` must match the value
provided on the payment.
