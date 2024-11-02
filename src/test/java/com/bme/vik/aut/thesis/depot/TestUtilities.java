package com.bme.vik.aut.thesis.depot;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestUtilities {

    public static void assertCreatedAndUpdatedTimes(LocalDateTime createdAt, LocalDateTime updatedAt) {

        assertNotNull(createdAt, "createdAt should not be null");
        assertNotNull(updatedAt, "updatedAt should not be null");

        LocalDateTime nowTruncated = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        LocalDateTime createdTruncated = createdAt.truncatedTo(ChronoUnit.MILLIS);
        LocalDateTime updatedTruncated = updatedAt.truncatedTo(ChronoUnit.MILLIS);

        assertTrue(ChronoUnit.SECONDS.between(nowTruncated, createdTruncated) < 1, "createdAt should be recent");
        assertTrue(ChronoUnit.SECONDS.between(nowTruncated, updatedTruncated) < 1, "updatedAt should be recent");
    }
}
