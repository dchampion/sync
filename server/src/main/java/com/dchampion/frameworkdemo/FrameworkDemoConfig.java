package com.dchampion.frameworkdemo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * App configuration.
 */
@Configuration
public class FrameworkDemoConfig {

    @Value("${framework-demo.bcrypt-strength}")
    private String value;

    @Bean
    public PasswordEncoder encoder() {
        return new BCryptPasswordEncoder(Integer.parseInt(value));
    }
}