package org.example.controller;

import jakarta.validation.Validation;
import org.example.dto.CreatePaymentRequest;
import org.example.dto.PageResponse;
import org.example.dto.PatchPaymentRequest;
import org.example.dto.PaymentResponse;
import org.example.dto.UpdatePaymentRequest;
import org.example.model.Currency;
import org.example.model.PaymentStatus;
import org.example.service.PaymentProcessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.SpringValidatorAdapter;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PaymentControllerTest {

    private static final UUID REFERENCE = UUID.fromString("8a2dd71f-ccaa-4c49-82ea-17222b6ecfee");

    private PaymentProcessor processor;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        processor = mock(PaymentProcessor.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new PaymentController(processor))
                .setControllerAdvice(new ApiExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setValidator(new SpringValidatorAdapter(
                        Validation.buildDefaultValidatorFactory().getValidator()
                ))
                .build();
    }

    @Test
    void postCreatesPayment() throws Exception {
        when(processor.createPayment(any(CreatePaymentRequest.class))).thenReturn(response());

        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "paymentType": "DOMESTIC_FEE",
                                  "amountInCents": 10000,
                                  "currency": "JOD",
                                  "notificationChannels": ["EMAIL"]
                                }
                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reference").value(REFERENCE.toString()))
                .andExpect(jsonPath("$.feeInCents").value(150));
    }

    @Test
    void getReturnsPayment() throws Exception {
        when(processor.getPayment(REFERENCE)).thenReturn(response());

        mockMvc.perform(get("/api/payments/{reference}", REFERENCE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentType").value("DOMESTIC_FEE"))
                .andExpect(jsonPath("$.status").value("PROCESSED"));
    }

    @Test
    void optionsReturnsTheRegisteredStrategies() throws Exception {
        when(processor.getAvailablePaymentTypes()).thenReturn(List.of(
                new org.example.model.PaymentType("DOMESTIC_FEE"),
                new org.example.model.PaymentType("CHEQUE_FEE")
        ));
        when(processor.getAvailableNotificationChannels()).thenReturn(List.of(
                new org.example.model.NotificationChannel("EMAIL"),
                new org.example.model.NotificationChannel("SMS")
        ));

        mockMvc.perform(get("/api/payments/options"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentTypes[0]").value("DOMESTIC_FEE"))
                .andExpect(jsonPath("$.notificationChannels[1]").value("SMS"));
    }

    @Test
    void putReplacesThePayment() throws Exception {
        when(processor.replacePayment(eq(REFERENCE), any(UpdatePaymentRequest.class))).thenReturn(response());

        mockMvc.perform(put("/api/payments/{reference}", REFERENCE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "paymentType": "CHEQUE_FEE",
                                  "amountInCents": 25000,
                                  "currency": "USD",
                                  "notificationChannels": ["SMS"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reference").value(REFERENCE.toString()));

        ArgumentCaptor<UpdatePaymentRequest> requestCaptor = ArgumentCaptor.forClass(UpdatePaymentRequest.class);
        verify(processor).replacePayment(eq(REFERENCE), requestCaptor.capture());
        assertEquals("CHEQUE_FEE", requestCaptor.getValue().paymentType());
        assertEquals(new BigDecimal("25000"), requestCaptor.getValue().amountInCents());
        assertEquals(Currency.USD, requestCaptor.getValue().currency());
        assertEquals(List.of("SMS"), requestCaptor.getValue().notificationChannels());
    }

    @Test
    void patchPartiallyUpdatesThePayment() throws Exception {
        when(processor.patchPayment(eq(REFERENCE), any(PatchPaymentRequest.class))).thenReturn(response());

        mockMvc.perform(patch("/api/payments/{reference}", REFERENCE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amountInCents": 10000}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.feeInCents").value(150));

        ArgumentCaptor<PatchPaymentRequest> requestCaptor = ArgumentCaptor.forClass(PatchPaymentRequest.class);
        verify(processor).patchPayment(eq(REFERENCE), requestCaptor.capture());
        assertEquals(new BigDecimal("10000"), requestCaptor.getValue().amountInCents());
        assertNull(requestCaptor.getValue().paymentType());
        assertNull(requestCaptor.getValue().currency());
        assertNull(requestCaptor.getValue().notificationChannels());
    }

    @Test
    void collectionSupportsFiltersAndPagination() throws Exception {
        when(processor.findPayments(
                eq(PaymentStatus.PROCESSED),
                eq(Currency.JOD),
                eq("DOMESTIC_FEE"),
                any(Pageable.class)
        )).thenReturn(new PageResponse<>(List.of(response()), 1, 5, 6, 2, false, true));

        mockMvc.perform(get("/api/payments")
                        .param("status", "PROCESSED")
                        .param("currency", "JOD")
                        .param("paymentType", "DOMESTIC_FEE")
                        .param("page", "1")
                        .param("size", "5")
                        .param("sort", "amountInCents,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.content[0].reference").value(REFERENCE.toString()));

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(processor).findPayments(
                eq(PaymentStatus.PROCESSED),
                eq(Currency.JOD),
                eq("DOMESTIC_FEE"),
                pageableCaptor.capture()
        );
        Pageable pageable = pageableCaptor.getValue();
        assertEquals(1, pageable.getPageNumber());
        assertEquals(5, pageable.getPageSize());
        assertEquals(Sort.Direction.ASC, pageable.getSort().getOrderFor("amountInCents").getDirection());
    }

    @Test
    void invalidPostReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "paymentType": "",
                                  "amountInCents": 0,
                                  "currency": "JOD",
                                  "notificationChannels": []
                                }
                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message").value("Request validation failed"))
                .andExpect(jsonPath("$.path").value("/api/payments"))
                .andExpect(jsonPath("$.violations.length()").value(3));
    }

    @Test
    void missingPaymentReturnsControlledNotFoundResponse() throws Exception {
        when(processor.getPayment(REFERENCE)).thenThrow(new java.util.NoSuchElementException(
                "Payment not found: " + REFERENCE
        ));

        mockMvc.perform(get("/api/payments/{reference}", REFERENCE))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PAYMENT_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Payment not found: " + REFERENCE))
                .andExpect(jsonPath("$.violations").isEmpty());
    }

    @Test
    void invalidDomainInputReturnsControlledBadRequestResponse() throws Exception {
        when(processor.patchPayment(eq(REFERENCE), any(PatchPaymentRequest.class)))
                .thenThrow(new IllegalArgumentException("At least one payment field must be supplied"));

        mockMvc.perform(patch("/api/payments/{reference}", REFERENCE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value("At least one payment field must be supplied"));
    }

    @Test
    void deleteReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/payments/{reference}", REFERENCE))
                .andExpect(status().isNoContent());

        verify(processor).deletePayment(REFERENCE);
    }

    @Test
    void unknownRoutePreservesFrameworkNotFoundStatus() throws Exception {
        mockMvc.perform(get("/api/not-a-route"))
                .andExpect(status().isNotFound());
    }

    @Test
    void unsupportedMethodPreservesFrameworkMethodNotAllowedStatus() throws Exception {
        mockMvc.perform(post("/api/payments/{reference}", REFERENCE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void unsupportedMediaTypePreservesFrameworkStatus() throws Exception {
        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("not json"))
                .andExpect(status().isUnsupportedMediaType());
    }

    private PaymentResponse response() {
        return new PaymentResponse(
                REFERENCE,
                "DOMESTIC_FEE",
                new BigDecimal("10000"),
                Currency.JOD,
                List.of("EMAIL"),
                new BigDecimal("150"),
                new BigDecimal("10150"),
                PaymentStatus.PROCESSED
        );
    }
}
