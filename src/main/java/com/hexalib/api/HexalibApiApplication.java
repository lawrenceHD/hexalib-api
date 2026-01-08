package com.hexalib.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories(basePackages = "com.hexalib.api")
public class HexalibApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(HexalibApiApplication.class, args);
    }

}