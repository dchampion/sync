package com.dchampion.http;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The application entry point.
 */
@SpringBootApplication
public class LongCallWithPollingDemo {
	public static void main(String[] args) {
		SpringApplication.run(LongCallWithPollingDemo.class, args);
	}
}
