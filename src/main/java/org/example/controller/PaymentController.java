package org.example.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
import java.util.ArrayList;
import java.util.UUID;

import static org.springframework.data.domain.Sort.Direction.DESC;

@RestController
@RequestMapping("/api/payments")
@Tag(name = "Payments", description = "Process payments and manage stored payment records")
public class PaymentController {

    private final PaymentProcessor paymentProcessor;

    public PaymentController(PaymentProcessor paymentProcessor) {
        this.paymentProcessor = paymentProcessor;
    }

    @PostMapping
    @Operation(summary = "Process a payment", description = "Calculates the fee, stores the payment, and sends selected notifications.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Payment processed"),
            @ApiResponse(responseCode = "400", description = "Invalid payment request")
    })
    public ResponseEntity<PaymentResponse> createPayment(@Valid @RequestBody CreatePaymentRequest request) {
        PaymentResponse response = paymentProcessor.createPayment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "Search payments", description = "Filters and pages stored payments.")
    @ApiResponse(responseCode = "200", description = "Payment page returned")
    public PageResponse<PaymentResponse> findPayments(
            @RequestParam(required = false) PaymentStatus status,
            @RequestParam(required = false) Currency currency,
            @RequestParam(required = false) String paymentType,
            @PageableDefault(size = 20, sort = "reference", direction = DESC) Pageable pageable) {
        return paymentProcessor.findPayments(status, currency, paymentType, pageable);
    }

    @GetMapping("/{reference}")
    @Operation(summary = "Get a payment by reference")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Payment returned"),
            @ApiResponse(responseCode = "404", description = "Payment not found")
    })
    public PaymentResponse getPayment(@PathVariable UUID reference) {
        return paymentProcessor.getPayment(reference);
    }

    @GetMapping("/options")
    @Operation(summary = "List payment options", description = "Returns registered payment types and notification channels.")
    @ApiResponse(responseCode = "200", description = "Options returned")
    public PaymentOptionsResponse getPaymentOptions() {
        List<String> paymentTypes = new ArrayList<>();
        for (PaymentType paymentType : paymentProcessor.getAvailablePaymentTypes()) {
            paymentTypes.add(paymentType.getName());
        }
        List<String> notificationChannels = new ArrayList<>();
        for (NotificationChannel channel : paymentProcessor.getAvailableNotificationChannels()) {
            notificationChannels.add(channel.getName());
        }
        return new PaymentOptionsResponse(paymentTypes, notificationChannels);
    }

    @PutMapping("/{reference}")
    @Operation(summary = "Replace a payment", description = "Replaces all editable details, recalculates the fee, and sends notifications.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Payment replaced"),
            @ApiResponse(responseCode = "400", description = "Invalid payment request"),
            @ApiResponse(responseCode = "404", description = "Payment not found")
    })
    public PaymentResponse replacePayment(@PathVariable UUID reference, @Valid @RequestBody UpdatePaymentRequest request) {
        return paymentProcessor.replacePayment(reference, request);
    }

    @PatchMapping("/{reference}")
    @Operation(summary = "Partially update a payment", description = "Updates supplied fields, recalculates the fee, and sends notifications.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Payment updated"),
            @ApiResponse(responseCode = "400", description = "Invalid payment request"),
            @ApiResponse(responseCode = "404", description = "Payment not found")
    })
    public PaymentResponse patchPayment(@PathVariable UUID reference, @Valid @RequestBody PatchPaymentRequest request) {
        return paymentProcessor.patchPayment(reference, request);
    }

    @DeleteMapping("/{reference}")
    @Operation(summary = "Delete a payment")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Payment deleted"),
            @ApiResponse(responseCode = "404", description = "Payment not found")
    })
    public ResponseEntity<Void> deletePayment(@PathVariable UUID reference) {
        paymentProcessor.deletePayment(reference);
        return ResponseEntity.noContent().build();
    }
}
