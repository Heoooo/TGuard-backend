package com.tguard.tguard_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

@SpringBootApplication
@EnableKafka
public class TguardBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(TguardBackendApplication.class, args);
	}

}
