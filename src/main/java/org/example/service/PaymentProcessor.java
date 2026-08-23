package org.example.service;

import org.example.dto.CreatePaymentRequest;
import org.example.dto.PageResponse;
import org.example.dto.PatchPaymentRequest;
import org.example.dto.PaymentResponse;
import org.example.dto.UpdatePaymentRequest;
import org.example.model.Currency;
import org.example.model.NotificationChannel;
import org.example.model.Payment;
import org.example.model.PaymentStatus;
import org.example.model.PaymentType;
import org.example.repo.PaymentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class PaymentProcessor {

    private final FeeCalculator feeCalculator;
    private final List<NotificationSender> notificationSenders;
    private final PaymentRepository paymentRepository;
    private static final Set<String> SORTABLE_PAYMENT_FIELDS = Set.of(
            "reference",
            "type.name",
            "amountInCents",
            "currency",
            "status",
            "feeInCents"
    );

    public PaymentProcessor(FeeCalculator feeCalculator, List<NotificationSender> notificationSenders,
                            PaymentRepository paymentRepository) {
        this.feeCalculator = feeCalculator;
        this.notificationSenders = List.copyOf(notificationSenders);
        this.paymentRepository = paymentRepository;
    }

    @Transactional
    public PaymentResponse createPayment(CreatePaymentRequest request) {
        Payment payment = new Payment(
                new PaymentType(request.paymentType()),
                request.amountInCents(),
                request.currency(),
                toNotificationChannels(request.notificationChannels())
        );
        return PaymentResponse.from(process(payment));
    }

    @Transactional
    public Payment process(Payment payment) {
        validateUniqueChannels(payment.getNotificationChannels());
        calculateAndAssignFee(payment);
        validateSupportedChannels(payment.getNotificationChannels());
        paymentRepository.saveAndFlush(payment);
        notifySelectedChannels(payment);
        payment.markAsProcessed();
        return payment;
    }

    public PaymentResponse getPayment(UUID reference) {
        return PaymentResponse.from(findPayment(reference));
    }

    public PageResponse<PaymentResponse> findPayments(PaymentStatus status, Currency currency,
                                                      String paymentType, Pageable pageable) {
        validateSort(pageable.getSort());
        String normalizedType = paymentType;
        if (paymentType == null || paymentType.isBlank()) {
            normalizedType = null;
        }
        Page<PaymentResponse> responses = paymentRepository
                .search(status, currency, normalizedType, pageable)
                .map(PaymentResponse::from);
        return PageResponse.from(responses);
    }

    @Transactional
    public PaymentResponse replacePayment(UUID reference, UpdatePaymentRequest request) {
        Payment payment = findPayment(reference);
        List<NotificationChannel> channels = toNotificationChannels(request.notificationChannels());
        validateUniqueChannels(channels);
        validateSupportedChannels(channels);
        payment.replaceDetails(
                new PaymentType(request.paymentType()),
                request.amountInCents(),
                request.currency(),
                channels
        );
        reprocessManagedPayment(payment);
        return PaymentResponse.from(payment);
    }

    @Transactional
    public PaymentResponse patchPayment(UUID reference, PatchPaymentRequest request) {
        if (request.hasNoChanges()) {
            throw new IllegalArgumentException("At least one payment field must be supplied");
        }

        Payment payment = findPayment(reference);
        PaymentType type = payment.getType();
        if (request.paymentType() != null) {
            type = new PaymentType(request.paymentType());
        }
        BigDecimal amount = payment.getAmountInCents();
        if (request.amountInCents() != null) {
            amount = request.amountInCents();
        }
        Currency currency = payment.getCurrency();
        if (request.currency() != null) {
            currency = request.currency();
        }
        List<NotificationChannel> channels = payment.getNotificationChannels();
        if (request.notificationChannels() != null) {
            channels = toNotificationChannels(request.notificationChannels());
        }

        validateUniqueChannels(channels);
        validateSupportedChannels(channels);
        payment.replaceDetails(type, amount, currency, channels);
        reprocessManagedPayment(payment);
        return PaymentResponse.from(payment);
    }

    @Transactional
    public void deletePayment(UUID reference) {
        Payment payment = findPayment(reference);
        paymentRepository.delete(payment);
    }

    public List<PaymentType> getAvailablePaymentTypes() {
        return feeCalculator.getAvailableTypes();
    }

    public List<NotificationChannel> getAvailableNotificationChannels() {
        List<NotificationChannel> channels = new ArrayList<>();
        for (NotificationSender sender : notificationSenders) {
            channels.add(sender.supportedChannel());
        }
        return List.copyOf(channels);
    }

    private Payment findPayment(UUID reference) {
        Optional<Payment> payment = paymentRepository.findByReference(reference);
        if (payment.isEmpty()) {
            throw new NoSuchElementException("Payment not found: " + reference);
        }
        return payment.get();
    }

    private void reprocessManagedPayment(Payment payment) {
        payment.prepareForProcessing();
        calculateAndAssignFee(payment);
        notifySelectedChannels(payment);
        payment.markAsProcessed();
    }

    private void calculateAndAssignFee(Payment payment) {
        payment.setFeeInCents(feeCalculator.calculateFee(payment));
    }

    private void notifySelectedChannels(Payment payment) {
        for (NotificationChannel channel : payment.getNotificationChannels()) {
            findNotificationSender(channel).send(payment);
        }
    }

    private NotificationSender findNotificationSender(NotificationChannel channel) {
        for (NotificationSender sender : notificationSenders) {
            if (sender.supportedChannel().getName().equals(channel.getName())) {
                return sender;
            }
        }
        throw new IllegalArgumentException("Unsupported notification channel: " + channel.getName());
    }

    private List<NotificationChannel> toNotificationChannels(List<String> names) {
        return names.stream().map(NotificationChannel::new).toList();
    }

    private void validateUniqueChannels(List<NotificationChannel> channels) {
        Set<String> names = new HashSet<>();
        for (NotificationChannel channel : channels) {
            if (!names.add(channel.getName())) {
                throw new IllegalArgumentException(
                        "Notification channel selected more than once: " + channel.getName()
                );
            }
        }
    }

    private void validateSupportedChannels(List<NotificationChannel> channels) {
        for (NotificationChannel channel : channels) {
            findNotificationSender(channel);
        }
    }

    private void validateSort(Sort sort) {
        for (Sort.Order order : sort) {
            if (!SORTABLE_PAYMENT_FIELDS.contains(order.getProperty())) {
                throw new IllegalArgumentException("Invalid sort property: " + order.getProperty());
            }
        }
    }
}
