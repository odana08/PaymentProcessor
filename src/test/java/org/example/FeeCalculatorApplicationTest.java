package org.example;

import org.example.controller.PaymentController;
import org.example.dto.PaymentResponse;
import org.example.model.Currency;
import org.example.model.Payment;
import org.example.model.PaymentStatus;
import org.example.repo.PaymentRepository;
import org.example.service.PaymentProcessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ActiveProfiles;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.math.BigDecimal;
import java.util.UUID;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class FeeCalculatorApplicationTest {

    @Autowired
    private PaymentController paymentController;

    @Autowired
    private PaymentProcessor paymentProcessor;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${local.server.port}")
    private int port;

    @BeforeEach
    void clearPayments() {
        paymentRepository.deleteAll();
    }

    @Test
    void applicationContextProvidesControllerServiceAndRepositoryBeans() {
        assertNotNull(paymentController);
        assertNotNull(paymentProcessor);
        assertNotNull(paymentRepository);
    }

    @Test
    void realHttpRequestTravelsThroughTheFullPersistenceStack() throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        String baseUrl = "http://localhost:" + port + "/api/payments";
        HttpRequest createRequest = HttpRequest.newBuilder(URI.create(baseUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("""
                        {
                          "paymentType": "INTERNATIONAL_FEE",
                          "amountInCents": 1000,
                          "currency": "JOD",
                          "notificationChannels": ["EMAIL"]
                        }
                        """))
                .build();

        HttpResponse<String> created = client.send(createRequest, HttpResponse.BodyHandlers.ofString());

        assertEquals(201, created.statusCode());
        PaymentResponse createdBody = readPaymentResponse(created);
        assertDecimalEquals("20.00", createdBody.feeInCents());
        assertEquals(PaymentStatus.PROCESSED, createdBody.status());
        UUID reference = createdBody.reference();

        Payment stored = paymentRepository.findById(reference).orElseThrow();
        assertEquals("INTERNATIONAL_FEE", stored.getType().getName());
        assertDecimalEquals("20.00", stored.getFeeInCents());

        HttpRequest getRequest = HttpRequest.newBuilder(URI.create(baseUrl + "/" + reference)).GET().build();
        HttpResponse<String> found = client.send(getRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, found.statusCode());
        PaymentResponse foundBody = readPaymentResponse(found);
        assertEquals(reference, foundBody.reference());
        assertEquals(PaymentStatus.PROCESSED, foundBody.status());

        HttpRequest replaceRequest = HttpRequest.newBuilder(URI.create(baseUrl + "/" + reference))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString("""
                        {
                          "paymentType": "CHEQUE_FEE",
                          "amountInCents": 25000,
                          "currency": "USD",
                          "notificationChannels": ["SMS"]
                        }
                        """))
                .build();
        HttpResponse<String> replaced = client.send(replaceRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, replaced.statusCode());
        PaymentResponse replacedBody = readPaymentResponse(replaced);
        assertEquals("CHEQUE_FEE", replacedBody.paymentType());
        assertEquals(Currency.USD, replacedBody.currency());
        assertEquals(java.util.List.of("SMS"), replacedBody.notificationChannels());
        assertDecimalEquals("25000", replacedBody.amountInCents());
        assertDecimalEquals("500", replacedBody.feeInCents());

        Payment storedReplacement = paymentRepository.findByReference(reference).orElseThrow();
        assertEquals("CHEQUE_FEE", storedReplacement.getType().getName());
        assertEquals(Currency.USD, storedReplacement.getCurrency());
        assertEquals("SMS", storedReplacement.getNotificationChannels().getFirst().getName());
        assertDecimalEquals("25000", storedReplacement.getAmountInCents());

        HttpRequest patchRequest = HttpRequest.newBuilder(URI.create(baseUrl + "/" + reference))
                .header("Content-Type", "application/json")
                .method("PATCH", HttpRequest.BodyPublishers.ofString("""
                        {"amountInCents": 50001}
                        """))
                .build();
        HttpResponse<String> patched = client.send(patchRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, patched.statusCode());
        PaymentResponse patchedBody = readPaymentResponse(patched);
        assertEquals("CHEQUE_FEE", patchedBody.paymentType());
        assertEquals(Currency.USD, patchedBody.currency());
        assertEquals(java.util.List.of("SMS"), patchedBody.notificationChannels());
        assertDecimalEquals("50001", patchedBody.amountInCents());
        assertDecimalEquals("1000", patchedBody.feeInCents());

        Payment storedPatch = paymentRepository.findByReference(reference).orElseThrow();
        assertDecimalEquals("50001", storedPatch.getAmountInCents());
        assertDecimalEquals("1000", storedPatch.getFeeInCents());

        URI collectionUri = URI.create(baseUrl
                + "?status=PROCESSED&currency=USD&paymentType=cheque_fee&page=0&size=1");
        HttpResponse<String> collection = client.send(
                HttpRequest.newBuilder(collectionUri).GET().build(),
                HttpResponse.BodyHandlers.ofString()
        );
        assertEquals(200, collection.statusCode());
        JsonNode collectionBody = objectMapper.readTree(collection.body());
        assertEquals(1, collectionBody.required("totalElements").longValue());
        assertEquals(1, collectionBody.required("content").size());
        assertEquals(
                reference.toString(),
                collectionBody.required("content").required(0).required("reference").stringValue()
        );

        HttpRequest deleteRequest = HttpRequest.newBuilder(URI.create(baseUrl + "/" + reference)).DELETE().build();
        HttpResponse<String> deleted = client.send(deleteRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(204, deleted.statusCode());
        assertFalse(paymentRepository.existsById(reference));
    }

    @Test
    void invalidSortUsesTheControlledBadRequestResponse() throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        String baseUrl = "http://localhost:" + port + "/api/payments";
        HttpResponse<String> response = client.send(
                HttpRequest.newBuilder(URI.create(baseUrl + "?sort=notAField")).GET().build(),
                HttpResponse.BodyHandlers.ofString()
        );

        assertEquals(400, response.statusCode());
        JsonNode error = objectMapper.readTree(response.body());
        assertEquals(400, error.required("status").intValue());
        assertEquals("Bad Request", error.required("error").stringValue());
        assertEquals("INVALID_REQUEST", error.required("code").stringValue());
        assertEquals("Invalid sort property: notAField", error.required("message").stringValue());
        assertEquals("/api/payments", error.required("path").stringValue());
    }

    @Test
    void openApiDocumentDescribesThePaymentEndpoints() throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> response = client.send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/v3/api-docs"))
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.ofString()
        );

        assertEquals(200, response.statusCode());
        JsonNode document = objectMapper.readTree(response.body());
        assertEquals("Fee Calculator API", document.required("info").required("title").stringValue());
        assertNotNull(document.required("paths").get("/api/payments"));
        assertNotNull(document.required("paths").get("/api/payments/{reference}"));
        assertNotNull(document.required("paths").get("/api/payments/options"));
    }

    private PaymentResponse readPaymentResponse(HttpResponse<String> response) {
        return objectMapper.readValue(response.body(), PaymentResponse.class);
    }

    private void assertDecimalEquals(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual));
    }
}
