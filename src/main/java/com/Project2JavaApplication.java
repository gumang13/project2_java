package com;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class Project2JavaApplication {

    public static void main(String[] args) {
        SpringApplication.run(Project2JavaApplication.class, args);
    }

}
