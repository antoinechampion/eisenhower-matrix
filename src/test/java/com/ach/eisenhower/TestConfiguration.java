package com.ach.eisenhower;

import org.springframework.boot.test.context.TestComponent;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackages = "com.ach.eisenhower", includeFilters = @ComponentScan.Filter(classes = TestComponent.class))
public class TestConfiguration {
}