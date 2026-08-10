package org.example;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NotificationChannelTest {

    @Test
    void givenValidName_whenNotificationChannelCreated_thenExposesName() {
        NotificationChannel channel = new NotificationChannel("EMAIL");

        assertEquals("EMAIL", channel.getName());
        assertEquals("EMAIL", channel.toString());
    }

    @Test
    void givenNullName_whenNotificationChannelCreated_thenThrowsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new NotificationChannel(null)
        );

        assertEquals("Notification channel cannot be empty", exception.getMessage());
    }

    @Test
    void givenBlankName_whenNotificationChannelCreated_thenThrowsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new NotificationChannel("\t")
        );

        assertEquals("Notification channel cannot be empty", exception.getMessage());
    }
}
