package com.capitec.securefile;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class SecurefileApplication {

    static void main(String[] args) {
        SpringApplication.run(SecurefileApplication.class, args);
    }

}
