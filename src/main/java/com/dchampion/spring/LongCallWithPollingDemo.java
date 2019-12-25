package com.dchampion.spring;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class LongCallWithPollingDemo {
	public static void main(String[] args) {
		SpringApplication.run(LongCallWithPollingDemo.class, args);
	}
}
