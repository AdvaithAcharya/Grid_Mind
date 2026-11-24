package com.gridmind;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class GridmindApplication {

    public static void main(String[] args) {
        SpringApplication.run(GridmindApplication.class, args);
    }
}
