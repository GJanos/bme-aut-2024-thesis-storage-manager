package com.bme.vik.aut.thesis.depot.general.config;

import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GeneralConfig {
    @Bean
    public ModelMapper modelMapperBean() {
        return new ModelMapper();
    }
}
