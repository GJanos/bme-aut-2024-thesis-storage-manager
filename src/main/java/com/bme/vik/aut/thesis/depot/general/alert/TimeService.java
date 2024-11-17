package com.bme.vik.aut.thesis.depot.general.alert;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class TimeService {
    public LocalDateTime getCurrentTime() {
        return LocalDateTime.now();
    }
}