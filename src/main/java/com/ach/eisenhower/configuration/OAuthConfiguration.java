package com.ach.eisenhower.configuration;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.authorization.InMemoryOAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.JdbcRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.security.oauth2.server.authorization.token.DelegatingOAuth2TokenGenerator;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.JwtGenerator;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;
import org.springframework.security.oauth2.server.authorization.token.ResourceIdentifierAudienceTokenCustomizer;
import org.springframework.security.crypto.keygen.Base64StringKeyGenerator;
import org.springframework.security.crypto.keygen.StringKeyGenerator;
import org.springframework.security.web.authentication.AuthenticationConverter;
import org.springframework.util.StringUtils;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Configuration
public class OAuthConfiguration {

    @Bean
    public JWKSource<SecurityContext> jwkSource(
            @Value("${eisenhower.oauth.signing-key:}") String signingKey,
            Environment environment) {
        RSAKey rsaKey;
        if (StringUtils.hasText(signingKey)) {
            rsaKey = parseRsaKey(signingKey);
        } else {
            // Production blank signing key must fail closed
            String datasourceUrl = environment.getProperty("spring.datasource.url", "");
            boolean isH2 = datasourceUrl.contains("h2:mem") || datasourceUrl.contains("jdbc:h2:");
            boolean isDevOrTest = isH2 || environment.acceptsProfiles(Profiles.of("local", "test", "dev"));
            if (!isDevOrTest) {
                throw new IllegalStateException("eisenhower.oauth.signing-key must be configured in production");
            }
            rsaKey = generateEphemeralRsaKey();
        }
        JWKSet jwkSet = new JWKSet(rsaKey);
        return new ImmutableJWKSet<>(jwkSet);
    }

    private static RSAKey parseRsaKey(String signingKey) {
        try {
            String sanitized = signingKey.trim()
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replace("-----BEGIN RSA PRIVATE KEY-----", "")
                    .replace("-----END RSA PRIVATE KEY-----", "")
                    .replaceAll("\\s+", "");
            byte[] encoded = Base64.getDecoder().decode(sanitized);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encoded);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            RSAPrivateKey privateKey = (RSAPrivateKey) keyFactory.generatePrivate(keySpec);

            RSAPublicKey publicKey;
            if (privateKey instanceof RSAPrivateCrtKey crtKey) {
                RSAPublicKeySpec publicKeySpec = new RSAPublicKeySpec(crtKey.getModulus(), crtKey.getPublicExponent());
                publicKey = (RSAPublicKey) keyFactory.generatePublic(publicKeySpec);
            } else {
                throw new IllegalArgumentException("Private key must contain RSA CRT parameters to derive public key");
            }

            return new RSAKey.Builder(publicKey)
                    .privateKey(privateKey)
                    .keyID("eisenhower-oauth-key")
                    .build();
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse RSA private key from eisenhower.oauth.signing-key", e);
        }
    }

    private static RSAKey generateEphemeralRsaKey() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            KeyPair keyPair = keyPairGenerator.generateKeyPair();
            RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
            RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
            return new RSAKey.Builder(publicKey)
                    .privateKey(privateKey)
                    .keyID(UUID.randomUUID().toString())
                    .build();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate ephemeral RSA key pair", e);
        }
    }

    @Bean
    public OAuth2TokenGenerator<?> tokenGenerator(JWKSource<SecurityContext> jwkSource) {
        JwtGenerator jwtGenerator = new JwtGenerator(new NimbusJwtEncoder(jwkSource));
        OAuth2TokenCustomizer<JwtEncodingContext> libraryCustomizer = new ResourceIdentifierAudienceTokenCustomizer();
        jwtGenerator.setJwtCustomizer(context -> {
            libraryCustomizer.customize(context);
            if (OAuth2TokenType.ACCESS_TOKEN.equals(context.getTokenType())
                    && AuthorizationGrantType.REFRESH_TOKEN.equals(context.getAuthorizationGrantType())
                    && context.getAuthorization() != null) {
                OAuth2Authorization authorization = context.getAuthorization();
                Set<String> audiences = new LinkedHashSet<>();
                if (authorization.getAccessToken() != null && authorization.getAccessToken().getClaims() != null) {
                    Object audClaim = authorization.getAccessToken().getClaims().get("aud");
                    if (audClaim instanceof Collection<?> col) {
                        for (Object item : col) {
                            if (item != null) {
                                audiences.add(item.toString());
                            }
                        }
                    } else if (audClaim instanceof String str && StringUtils.hasText(str)) {
                        audiences.add(str);
                    }
                }
                if (audiences.isEmpty()) {
                    OAuth2AuthorizationRequest authRequest = authorization.getAttribute(OAuth2AuthorizationRequest.class.getName());
                    if (authRequest != null) {
                        Object resourceParam = authRequest.getAdditionalParameters().get("resource");
                        if (resourceParam instanceof Collection<?> col) {
                            for (Object item : col) {
                                if (item != null) {
                                    audiences.add(item.toString());
                                }
                            }
                        } else if (resourceParam instanceof String str && StringUtils.hasText(str)) {
                            audiences.add(str);
                        }
                    }
                }
                if (!audiences.isEmpty()) {
                    context.getClaims().audience(new ArrayList<>(audiences));
                }
            }
        });

        StringKeyGenerator keyGenerator = new Base64StringKeyGenerator(Base64.getUrlEncoder().withoutPadding(), 96);
        OAuth2TokenGenerator<OAuth2RefreshToken> refreshTokenGenerator = context -> {
            if (!OAuth2TokenType.REFRESH_TOKEN.equals(context.getTokenType())) {
                return null;
            }
            if (context.getRegisteredClient() == null ||
                    !context.getRegisteredClient().getAuthorizationGrantTypes().contains(org.springframework.security.oauth2.core.AuthorizationGrantType.REFRESH_TOKEN)) {
                return null;
            }
            Instant issuedAt = Instant.now();
            Instant expiresAt = issuedAt.plus(context.getRegisteredClient().getTokenSettings().getRefreshTokenTimeToLive());
            return new OAuth2RefreshToken(keyGenerator.generateKey(), issuedAt, expiresAt);
        };

        return new DelegatingOAuth2TokenGenerator(jwtGenerator, refreshTokenGenerator);
    }

    @Bean
    public AuthorizationServerSettings authorizationServerSettings(@Value("${eisenhower.app.base-url}") String baseUrl) {
        return AuthorizationServerSettings.builder()
                .issuer(baseUrl)
                .build();
    }

    @Bean
    public RegisteredClientRepository registeredClientRepository(JdbcTemplate jdbcTemplate) {
        JdbcRegisteredClientRepository delegate = new JdbcRegisteredClientRepository(jdbcTemplate);
        return new RegisteredClientRepository() {
            @Override
            public void save(RegisteredClient registeredClient) {
                // Restrict grant types to AUTHORIZATION_CODE and REFRESH_TOKEN
                for (AuthorizationGrantType grantType : registeredClient.getAuthorizationGrantTypes()) {
                    if (!AuthorizationGrantType.AUTHORIZATION_CODE.equals(grantType) &&
                            !AuthorizationGrantType.REFRESH_TOKEN.equals(grantType)) {
                        throw new OAuth2AuthenticationException(new OAuth2Error(
                                "invalid_client_metadata",
                                "Unsupported grant_type: " + grantType.getValue(),
                                "https://datatracker.ietf.org/doc/html/rfc7591#section-3.2.2"
                        ));
                    }
                }

                TokenSettings tokenSettings = TokenSettings.builder()
                        .accessTokenTimeToLive(Duration.ofHours(1))
                        .refreshTokenTimeToLive(Duration.ofDays(30))
                        .reuseRefreshTokens(false)
                        .build();
                RegisteredClient.Builder builder = RegisteredClient.from(registeredClient)
                        .tokenSettings(tokenSettings);
                if (!registeredClient.getAuthorizationGrantTypes().contains(AuthorizationGrantType.REFRESH_TOKEN)) {
                    builder.authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN);
                }
                delegate.save(builder.build());
            }

            @Override
            public RegisteredClient findById(String id) {
                return delegate.findById(id);
            }

            @Override
            public RegisteredClient findByClientId(String clientId) {
                return delegate.findByClientId(clientId);
            }
        };
    }

    @Bean
    public OAuth2AuthorizationService authorizationService(JdbcTemplate jdbcTemplate, RegisteredClientRepository registeredClientRepository) {
        return new JdbcOAuth2AuthorizationService(jdbcTemplate, registeredClientRepository);
    }

    @Bean
    public OAuth2AuthorizationConsentService authorizationConsentService() {
        return new InMemoryOAuth2AuthorizationConsentService();
    }

    /**
     * Converter to authenticate public clients (token_endpoint_auth_method=none) on refresh_token grant
     */
    public static class PublicClientRefreshTokenAuthenticationConverter implements AuthenticationConverter {
        @Override
        public Authentication convert(HttpServletRequest request) {
            String grantType = request.getParameter(OAuth2ParameterNames.GRANT_TYPE);
            if (!AuthorizationGrantType.REFRESH_TOKEN.getValue().equals(grantType)) {
                return null;
            }

            String clientId = request.getParameter(OAuth2ParameterNames.CLIENT_ID);
            if (!StringUtils.hasText(clientId)) {
                return null;
            }

            // Public client only: do not handle if client secret, Authorization header, or client assertion is present
            if (StringUtils.hasText(request.getParameter(OAuth2ParameterNames.CLIENT_SECRET)) ||
                    StringUtils.hasText(request.getHeader(HttpHeaders.AUTHORIZATION)) ||
                    StringUtils.hasText(request.getParameter(OAuth2ParameterNames.CLIENT_ASSERTION)) ||
                    StringUtils.hasText(request.getParameter(OAuth2ParameterNames.CLIENT_ASSERTION_TYPE))) {
                return null;
            }

            Map<String, Object> additionalParameters = new HashMap<>();
            request.getParameterMap().forEach((key, value) -> {
                if (!OAuth2ParameterNames.CLIENT_ID.equals(key)) {
                    additionalParameters.put(key, value.length == 1 ? value[0] : value);
                }
            });

            return new OAuth2ClientAuthenticationToken(
                    clientId,
                    ClientAuthenticationMethod.NONE,
                    null,
                    additionalParameters
            );
        }
    }

    /**
     * Provider to authenticate public clients on refresh_token grant
     */
    public static class PublicClientRefreshTokenAuthenticationProvider implements AuthenticationProvider {
        private final RegisteredClientRepository registeredClientRepository;

        public PublicClientRefreshTokenAuthenticationProvider(RegisteredClientRepository registeredClientRepository) {
            this.registeredClientRepository = registeredClientRepository;
        }

        @Override
        public Authentication authenticate(Authentication authentication) throws AuthenticationException {
            OAuth2ClientAuthenticationToken clientAuthentication = (OAuth2ClientAuthenticationToken) authentication;

            if (!ClientAuthenticationMethod.NONE.equals(clientAuthentication.getClientAuthenticationMethod())) {
                return null;
            }

            String grantType = (String) clientAuthentication.getAdditionalParameters().get(OAuth2ParameterNames.GRANT_TYPE);
            if (!AuthorizationGrantType.REFRESH_TOKEN.getValue().equals(grantType)) {
                return null;
            }

            String clientId = clientAuthentication.getPrincipal().toString();
            RegisteredClient registeredClient = this.registeredClientRepository.findByClientId(clientId);
            if (registeredClient == null) {
                throw new OAuth2AuthenticationException(new OAuth2Error(OAuth2ErrorCodes.INVALID_CLIENT, "Client not found", null));
            }

            if (!registeredClient.getClientAuthenticationMethods().contains(ClientAuthenticationMethod.NONE)) {
                throw new OAuth2AuthenticationException(new OAuth2Error(OAuth2ErrorCodes.INVALID_CLIENT, "Client is not configured for public authentication", null));
            }

            if (!registeredClient.getAuthorizationGrantTypes().contains(AuthorizationGrantType.REFRESH_TOKEN)) {
                throw new OAuth2AuthenticationException(new OAuth2Error(OAuth2ErrorCodes.UNAUTHORIZED_CLIENT, "Client is not authorized for refresh token grant", null));
            }

            return new OAuth2ClientAuthenticationToken(registeredClient, ClientAuthenticationMethod.NONE, null);
        }

        @Override
        public boolean supports(Class<?> authentication) {
            return OAuth2ClientAuthenticationToken.class.isAssignableFrom(authentication);
        }
    }
}
