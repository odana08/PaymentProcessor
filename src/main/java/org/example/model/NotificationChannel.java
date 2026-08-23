package org.example.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.util.Objects;

@Embeddable
public class NotificationChannel {

    @Column(name = "channel_name", nullable = false, length = 32)
    private String name;

    protected NotificationChannel() {
    }

    public NotificationChannel(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Notification channel cannot be empty");
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
        if (!(other instanceof NotificationChannel that)) {
            return false;
        }
        return name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}
