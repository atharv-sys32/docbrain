package com.docbrain;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class DocBrainApplication {

    public static void main(String[] args) {
        SpringApplication.run(DocBrainApplication.class, args);
    }
}
