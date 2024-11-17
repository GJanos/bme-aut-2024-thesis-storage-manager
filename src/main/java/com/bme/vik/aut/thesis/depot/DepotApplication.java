package com.bme.vik.aut.thesis.depot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DepotApplication {

    public static void main(String[] args) {
        SpringApplication.run(DepotApplication.class, args);
    }
}
