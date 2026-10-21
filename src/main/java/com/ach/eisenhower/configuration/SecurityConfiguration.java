package com.ach.eisenhower.configuration;

import com.ach.eisenhower.middlewares.JwtFilter;
import com.ach.eisenhower.middlewares.UnauthenticatedRequestHandler;
import com.ach.eisenhower.repositories.EisenhowerUserRepository;
import com.ach.eisenhower.services.UserTokenParserService;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import lombok.RequiredArgsConstructor;
import org.springaicommunity.mcp.security.authorizationserver.config.McpAuthorizationServerConfigurer;
import org.springaicommunity.mcp.security.server.oauth2.authentication.BearerResourceMetadataTokenAuthenticationEntryPoint;
import org.springaicommunity.mcp.security.server.oauth2.jwt.JwtResourceValidator;
import org.springaicommunity.mcp.security.server.oauth2.metadata.OAuth2ProtectedResourceMetadataEndpointFilter;
import org.springaicommunity.mcp.security.server.oauth2.metadata.ResourceIdentifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;

/**
 * Configures the Authentication and Security filter chains at the app level
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
     * Standard UserDetailsService for Spring Security authentication providers
     */
    @Bean
    public UserDetailsService addMcpServer(EisenhowerUserRepository userRepository) {
        return username -> {
            var user = userRepository.findByEmail(username)
                    .orElseThrow(() -> new UsernameNotFoundException("User does not exist: " + username));
            return new org.springframework.security.core.userdetails.User(
                    user.getEmail(),
                    user.getPassword(),
                    user.isEnabled(),
                    true,
                    true,
                    true,
                    user.getAuthorities()
            );
        };
    }

    /**
     * Defines the authentication manager at the app level
     */
    @Bean
    public AuthenticationManager authenticationManager(UserDetailsService userDetailsService, PasswordEncoder encoder) {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService);
        authProvider.setPasswordEncoder(encoder);
        return new ProviderManager(authProvider);
    }

    /**
     * OAuth2 Authorization Server filter chain for DCR (/oauth2/register), token issuance, and login
     */
    @Bean
    @Order(1)
    public SecurityFilterChain authorizationServerFilterChain(
            HttpSecurity http,
            RegisteredClientRepository registeredClientRepository) throws Exception {

        McpAuthorizationServerConfigurer mcpConfigurer = McpAuthorizationServerConfigurer.mcpAuthorizationServer()
                .authorizationServer(authServer -> authServer
                        .clientAuthentication(clientAuth -> clientAuth
                                .authenticationConverter(new OAuthConfiguration.PublicClientRefreshTokenAuthenticationConverter())
                                .authenticationProvider(new OAuthConfiguration.PublicClientRefreshTokenAuthenticationProvider(registeredClientRepository))
                        )
                );

        http
                .securityMatcher(
                        "/oauth2/**",
                        "/.well-known/oauth-authorization-server/**",
                        "/.well-known/openid-configuration/**",
                        "/login"
                )
                .with(mcpConfigurer, Customizer.withDefaults())
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/login").permitAll()
                        .anyRequest().authenticated()
                )
                .formLogin(Customizer.withDefaults());
        return http.build();
    }

    /**
     * MCP Resource Server filter chain protecting /mcp/** with audience-bound OAuth2 tokens
     */
    @Bean
    @Order(2)
    public SecurityFilterChain mcpFilterChain(
            HttpSecurity http,
            JWKSource<SecurityContext> jwkSource,
            @Value("${eisenhower.app.base-url}") String baseUrl) throws Exception {

        JwtDecoder jwtDecoder = localJwtDecoder(jwkSource, baseUrl);
        ResourceIdentifier resourceIdentifier = new ResourceIdentifier("/mcp");

        var protectedResourceMetadataEndpointFilter = new OAuth2ProtectedResourceMetadataEndpointFilter(resourceIdentifier);
        protectedResourceMetadataEndpointFilter.setProtectedResourceMetadataCustomizer(metadata ->
                metadata.authorizationServer(baseUrl)
                        .resourceName("Eisenhower Matrix MCP Server")
                        .bearerMethod("header")
        );

        var entryPoint = new BearerResourceMetadataTokenAuthenticationEntryPoint(resourceIdentifier);

        http
                .securityMatcher("/mcp/**", "/.well-known/oauth-protected-resource/**")
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/.well-known/oauth-protected-resource/**").permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .jwt(jwt -> jwt.decoder(jwtDecoder))
                        .authenticationEntryPoint(entryPoint)
                )
                .addFilterBefore(protectedResourceMetadataEndpointFilter, AbstractPreAuthenticatedProcessingFilter.class);

        return http.build();
    }

    private JwtDecoder localJwtDecoder(JWKSource<SecurityContext> jwkSource, String baseUrl) {
        NimbusJwtDecoder decoder = (NimbusJwtDecoder) OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);

        OAuth2TokenValidator<Jwt> defaultValidator = JwtValidators.createDefaultWithValidators(
                new JwtIssuerValidator(baseUrl),
                new JwtResourceValidator(new ResourceIdentifier("/mcp"))
        );
        decoder.setJwtValidator(defaultValidator);
        return decoder;
    }

    /**
     * Defines authenticated routes and security middlewares for SPA
     */
    @Bean
    @Order(3)
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .httpBasic(httpBasic -> httpBasic.disable())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(c -> c.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(c -> c.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/api/attached-document/**").permitAll()
                        .requestMatchers("/api/**").authenticated()
                        .requestMatchers("/matrix**").authenticated()
                        .requestMatchers("/auth/user-details").authenticated()
                        .anyRequest().permitAll()
                )
                .addFilterBefore(new JwtFilter(userTokenParserService), UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(c -> c.authenticationEntryPoint(new UnauthenticatedRequestHandler()));
        return http.build();
    }
}
