package com.ukbank.fraudplatform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
public class BankingFraudPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(BankingFraudPlatformApplication.class, args);
    }
}
