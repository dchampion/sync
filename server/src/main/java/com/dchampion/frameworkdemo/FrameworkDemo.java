package com.dchampion.frameworkdemo;

import com.dchampion.framework.Framework;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The application entry point.
 */
@SpringBootApplication(scanBasePackageClasses={FrameworkDemo.class,Framework.class})
public class FrameworkDemo {
	public static void main(String[] args) {
		SpringApplication.run(FrameworkDemo.class, args);
	}
}
