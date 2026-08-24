package com.training;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TrainingManagementSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(TrainingManagementSystemApplication.class, args);
        System.out.println("Training Management System Application");
        System.out.println("http://localhost:8080/swagger-ui/index.html");

    }

}
