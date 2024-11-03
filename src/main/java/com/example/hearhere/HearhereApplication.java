package com.example.hearhere;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class HearhereApplication {

	public static void main(String[] args) {
		SpringApplication.run(HearhereApplication.class, args);
	}

}
