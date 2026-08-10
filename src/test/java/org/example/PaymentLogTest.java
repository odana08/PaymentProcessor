package org.example;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class PaymentLogTest {

    @Test
    void givenNewLog_whenRecordsRequested_thenReturnsEmptyList() {
        PaymentLog log = new PaymentLog();

        assertTrue(log.getRecords().isEmpty());
    }

    @Test
    void givenPaymentRecord_whenSaved_thenRecordIsAvailable() {
        PaymentLog log = new PaymentLog();
        PaymentRecord record = mock(PaymentRecord.class);

        log.save(record);

        assertEquals(1, log.getRecords().size());
        assertSame(record, log.getRecords().getFirst());
    }
}
