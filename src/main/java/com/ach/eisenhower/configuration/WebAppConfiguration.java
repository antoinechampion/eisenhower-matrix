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
            public void addInterceptors(@NonNull InterceptorRegistry registry) {
                registry.addInterceptor(new HomePageRedirectHandler()).addPathPatterns("/");
                registry.addInterceptor(new UserVisitInterceptor(userRepository)).addPathPatterns("/matrix");
            }

            @Override
            public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {
                // Serve static files from classpath:/static/
                registry.addResourceHandler("/**")
                        .addResourceLocations("classpath:/static/")
                        .resourceChain(false);
            }

            @Override
            public void addViewControllers(@NonNull ViewControllerRegistry registry) {
                // Forward all non-API/auth routes to index.html for client-side routing
                // This allows Vue Router to handle the routing
                registry.addViewController("/{spring:\\w+}")
                        .setViewName("forward:/index.html");
            }
        };
    }
}
