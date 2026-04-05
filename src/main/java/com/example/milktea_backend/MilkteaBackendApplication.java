package com.example.milktea_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class MilkteaBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(MilkteaBackendApplication.class, args);
	}

}
