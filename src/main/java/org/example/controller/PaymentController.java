package org.example.controller;

import jakarta.validation.Valid;
import org.example.dto.CreatePaymentRequest;
import org.example.dto.PageResponse;
import org.example.dto.PatchPaymentRequest;
import org.example.dto.PaymentOptionsResponse;
import org.example.dto.PaymentResponse;
import org.example.dto.UpdatePaymentRequest;
import org.example.model.Currency;
import org.example.model.NotificationChannel;
import org.example.model.PaymentStatus;
import org.example.model.PaymentType;
import org.example.service.PaymentProcessor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

import static org.springframework.data.domain.Sort.Direction.DESC;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentProcessor paymentProcessor;

    public PaymentController(PaymentProcessor paymentProcessor) {
        this.paymentProcessor = paymentProcessor;
    }

    @PostMapping
    public ResponseEntity<PaymentResponse> createPayment(@Valid @RequestBody CreatePaymentRequest request) {
        PaymentResponse response = paymentProcessor.createPayment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public PageResponse<PaymentResponse> findPayments(
            @RequestParam(required = false) PaymentStatus status,
            @RequestParam(required = false) Currency currency,
            @RequestParam(required = false) String paymentType,
            @PageableDefault(size = 20, sort = "reference", direction = DESC) Pageable pageable) {
        return paymentProcessor.findPayments(status, currency, paymentType, pageable);
    }

    @GetMapping("/{reference}")
    public PaymentResponse getPayment(@PathVariable UUID reference) {
        return paymentProcessor.getPayment(reference);
    }

    @GetMapping("/options")
    public PaymentOptionsResponse getPaymentOptions() {
        List<String> paymentTypes = paymentProcessor.getAvailablePaymentTypes().stream()
                .map(PaymentType::getName)
                .toList();
        List<String> notificationChannels = paymentProcessor.getAvailableNotificationChannels().stream()
                .map(NotificationChannel::getName)
                .toList();
        return new PaymentOptionsResponse(paymentTypes, notificationChannels);
    }

    @PutMapping("/{reference}")
    public PaymentResponse replacePayment(@PathVariable UUID reference, @Valid @RequestBody UpdatePaymentRequest request) {
        return paymentProcessor.replacePayment(reference, request);
    }

    @PatchMapping("/{reference}")
    public PaymentResponse patchPayment(@PathVariable UUID reference, @Valid @RequestBody PatchPaymentRequest request) {
        return paymentProcessor.patchPayment(reference, request);
    }

    @DeleteMapping("/{reference}")
    public ResponseEntity<Void> deletePayment(@PathVariable UUID reference) {
        paymentProcessor.deletePayment(reference);
        return ResponseEntity.noContent().build();
    }
}
