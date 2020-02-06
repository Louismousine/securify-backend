package com;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories

public class DemoApplication {
	
	// http://localhost:8090/swagger-ui.html#/
	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
	}

}
