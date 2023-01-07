package com.ach.eisenhower.configuration;

import com.ach.eisenhower.middlewares.JwtFilter;
import com.ach.eisenhower.middlewares.UnauthenticatedRequestHandler;
import com.ach.eisenhower.repositories.EisenhowerUserRepository;
import com.ach.eisenhower.services.UserTokenParserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Configures the Authentication at the app level
 */
@Configuration
@RequiredArgsConstructor
public class SecurityConfiguration {
    private final UserTokenParserService userTokenParserService;

    /**
     * Password encoder for the user entity
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Defines the authentication manager at the app level
     */
    @Bean
    AuthenticationManager customAuthenticationManager(EisenhowerUserRepository userRepository, PasswordEncoder encoder) {
        return authentication -> {
            String username = authentication.getPrincipal() + "";
            String password = authentication.getCredentials() + "";

            var user = userRepository.findByEmail(username);

            if (user.isEmpty()) {
                throw new UsernameNotFoundException("User does not exist");
            }

            if (!encoder.matches(password, user.get().getPassword())) {
                throw new BadCredentialsException("Bad credentials");
            }

            if (!user.get().isEnabled()) {
                throw new DisabledException("User account is not active");
            }

            return new UsernamePasswordAuthenticationToken(username, null, user.get().getAuthorities());
        };
    }

    /**
     * Defines authenticated routes and security middlewares
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .httpBasic().disable()
                .csrf().disable()
                .sessionManagement(c -> c.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(c -> c.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/**").authenticated()
                        .requestMatchers("/matrix**", "/editor**").authenticated()
                        .requestMatchers("/auth/user-details").authenticated()
                        .anyRequest().permitAll()
                )
                .addFilterBefore(new JwtFilter(userTokenParserService), UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling().authenticationEntryPoint(new UnauthenticatedRequestHandler());
        return http.build();
    }
}
