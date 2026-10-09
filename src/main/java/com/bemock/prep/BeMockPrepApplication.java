package com.bemock.prep;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class BeMockPrepApplication {

    public static void main(String[] args) {
        SpringApplication.run(BeMockPrepApplication.class, args);
    }
}
