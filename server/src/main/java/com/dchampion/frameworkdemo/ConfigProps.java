package com.dchampion.frameworkdemo;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "framework-demo")
public class ConfigProps {
    private String breachApiHashAlgo;
    private String breachApiURIRoot;

    public String getBreachApiHashAlgo() {
        return breachApiHashAlgo;
    }

    public void setBreachApiHashAlgo(String breachApiHashAlgo) {
        this.breachApiHashAlgo = breachApiHashAlgo;
    }

    public String getBreachApiURIRoot() {
        return breachApiURIRoot;
    }

    public void setBreachApiURIRoot(String breachApiURIRoot) {
        this.breachApiURIRoot = breachApiURIRoot;
    }
}