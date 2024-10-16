package com.bme.vik.aut.thesis.depot;

import org.springframework.boot.SpringApplication;

public class TestDepotApplication {

    public static void main(String[] args) {
        SpringApplication.from(DepotApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
