package com.bme.vik.aut.thesis.depot.general.alert;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(MockitoExtension.class)
class TimeServiceTest {

    @InjectMocks
    private TimeService timeService;

    @Test
    void shouldReturnCurrentTime() {
        //***** <-- given: Mocked current time --> *****//
        LocalDateTime mockedCurrentTime = LocalDateTime.of(2023, 11, 15, 10, 0, 0);
        Clock fixedClock = Clock.fixed(mockedCurrentTime.toInstant(ZoneOffset.UTC), ZoneId.of("UTC"));

        // Override the system clock temporarily
        try (MockedStatic<LocalDateTime> mockedStaticTime = Mockito.mockStatic(LocalDateTime.class)) {
            mockedStaticTime.when(LocalDateTime::now).thenReturn(mockedCurrentTime);

            //***** <-- when: Get current time --> *****//
            LocalDateTime actualTime = timeService.getCurrentTime();

            //***** <-- then: Validate returned time --> *****//
            assertNotNull(actualTime);
            assertEquals(mockedCurrentTime, actualTime);
        }
    }
}
