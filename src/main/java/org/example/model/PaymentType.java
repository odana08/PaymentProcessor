package org.example.model;

public class PaymentType {

    private final String name;

    public PaymentType(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Payment type cannot be empty");
        }

        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }
}
