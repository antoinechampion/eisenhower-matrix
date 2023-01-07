package com.ach.eisenhower;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories("com.ach.eisenhower.*")
@ComponentScan(basePackages = {"com.ach.eisenhower.*"})
@EntityScan("com.ach.eisenhower.entities")
public class ServerApplication {
    public static void main(String[] args) {

        SpringApplication.run(ServerApplication.class, args);
    }
}
