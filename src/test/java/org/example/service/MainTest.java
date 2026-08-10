package org.example.service;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MainTest {

    @Test
    void givenApplicationEntryPoint_whenMainInvoked_thenCompletesNormally() {
        InputStream originalInput = System.in;
        PrintStream originalOutput = System.out;
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        try {
            System.setIn(new ByteArrayInputStream("0\n".getBytes(StandardCharsets.UTF_8)));
            System.setOut(new PrintStream(output));

            assertDoesNotThrow(() -> Main.main(new String[0]));
        } finally {
            System.setIn(originalInput);
            System.setOut(originalOutput);
        }

        assertTrue(output.toString().contains("Fee Calculator"));
        assertTrue(output.toString().contains("Goodbye."));
    }
}
