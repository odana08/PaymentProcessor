package org.example.model;

public final class NotificationChannel {

    private final String name;

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
}
