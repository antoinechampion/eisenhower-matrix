package com.ach.eisenhower.configuration;

import com.ach.eisenhower.repositories.EisenhowerUserRepository;
import com.ach.eisenhower.middlewares.HomePageRedirectHandler;
import com.ach.eisenhower.middlewares.UserVisitInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.*;

/**
 * Configuration for the front-end application
 */
@Configuration
@RequiredArgsConstructor
public class WebAppConfiguration implements WebMvcConfigurer {

    private final EisenhowerUserRepository userRepository;

    /**
     * Routes the front-end pages to views and add routing middlewares
     */
    @Bean
    public WebMvcConfigurer appConfig() {

        return new WebMvcConfigurer() {
            @Override
            public void addViewControllers(@NonNull ViewControllerRegistry registry) {
                registry.addViewController("/")
                        .setViewName("forward:/index.html");
                registry.addViewController("/faq")
                        .setViewName("forward:/faq.html");
                registry.addViewController("/matrix")
                        .setViewName("forward:/matrix.html");
                registry.addViewController("/editor")
                        .setViewName("forward:/editor.html");
            }

            @Override
            public void addInterceptors(@NonNull InterceptorRegistry registry) {
                registry.addInterceptor(new HomePageRedirectHandler()).addPathPatterns("/");
                registry.addInterceptor(new UserVisitInterceptor(userRepository)).addPathPatterns("/matrix");
            }
        };
    }
}
