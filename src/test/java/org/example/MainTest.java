package org.example;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class MainTest {

    @Test
    void givenApplicationEntryPoint_whenMainInvoked_thenCompletesNormally() {
        assertDoesNotThrow(Main::main);
    }
}
