package com.lifelink;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point of the LifeLink application.
 * Scheduling is enabled for the periodic low-stock / expiry alert refresh.
 */
@SpringBootApplication
@EnableScheduling
public class LifeLinkApplication {

    public static void main(String[] args) {
        SpringApplication.run(LifeLinkApplication.class, args);
    }
}
