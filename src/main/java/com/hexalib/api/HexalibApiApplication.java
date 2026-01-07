package com.hexalib.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan("com.hexalib.api.auth.model")  // ou le package où sont tes @Entity
@EnableJpaRepositories("com.hexalib.api.auth.repository")  // ou le package où sont tes repositories
public class HexalibApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(HexalibApiApplication.class, args);
	}

}
