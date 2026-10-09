package com.bemock.prep;

import org.springframework.boot.SpringApplication;

/** Runs the app locally against a throwaway Testcontainers PostgreSQL (no docker compose needed). */
public class TestBeMockPrepApplication {

    public static void main(String[] args) {
        SpringApplication.from(BeMockPrepApplication::main).with(TestcontainersConfiguration.class).run(args);
    }
}
