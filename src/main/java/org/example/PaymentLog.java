package org.example;

import java.util.ArrayList;
import java.util.List;

public class PaymentLog {

    private final List<PaymentRecord> records = new ArrayList<>();

    public void save(PaymentRecord paymentRecord) {
        records.add(paymentRecord);
    }

    public List<PaymentRecord> getRecords() {
        return records;
    }
}