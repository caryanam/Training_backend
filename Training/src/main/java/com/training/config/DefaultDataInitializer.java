package com.training.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DefaultDataInitializer implements CommandLineRunner {
    @Override
    public void run(String... args) {
        // No default dummy data will be initialized.
    }
}
