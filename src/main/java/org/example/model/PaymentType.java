package org.example.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.util.Objects;

@Embeddable
public class PaymentType {

    @Column(name = "payment_type", nullable = false, length = 50)
    private String name;

    protected PaymentType() {
    }

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

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof PaymentType that)) {
            return false;
        }
        return name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}
