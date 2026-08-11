package com.libris;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class LibrisApplication {

    public static void main(String[] args) {
        SpringApplication.run(LibrisApplication.class, args);
    }
}
