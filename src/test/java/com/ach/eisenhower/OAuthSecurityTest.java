package com.ach.eisenhower;

import com.ach.eisenhower.configuration.OAuthConfiguration;
import com.ach.eisenhower.entities.EisenhowerUserEntity;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKMatcher;
import com.nimbusds.jose.jwk.JWKSelector;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.test.web.servlet.MvcResult;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.interfaces.RSAPrivateKey;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class OAuthSecurityTest extends EisenhowerBaseTest {

    @Autowired
    private RegisteredClientRepository registeredClientRepository;

    @Autowired
    private JWKSource<SecurityContext> jwkSource;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void testUnauthenticatedMcpRequestReturns401WithResourceMetadata() throws Exception {
        mvc.perform(post("/mcp"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().exists(HttpHeaders.WWW_AUTHENTICATE))
                .andExpect(result -> {
                    String authHeader = result.getResponse().getHeader(HttpHeaders.WWW_AUTHENTICATE);
                    assertThat(authHeader).contains("resource_metadata=");
                    assertThat(authHeader).contains("/.well-known/oauth-protected-resource/mcp");
                });
    }

    @Test
    public void testProtectedResourceMetadataEndpoint() throws Exception {
        mvc.perform(get("/.well-known/oauth-protected-resource/mcp"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resource").exists())
                .andExpect(jsonPath("$.authorization_servers[0]").value("http://localhost:8080"))
                .andExpect(jsonPath("$.bearer_methods_supported[0]").value("header"));
    }

    @Test
    public void testAuthorizationServerMetadataEndpoint() throws Exception {
        mvc.perform(get("/.well-known/oauth-authorization-server"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.issuer").value("http://localhost:8080"))
                .andExpect(jsonPath("$.authorization_endpoint").exists())
                .andExpect(jsonPath("$.token_endpoint").exists())
                .andExpect(jsonPath("$.jwks_uri").exists())
                .andExpect(jsonPath("$.registration_endpoint").exists())
                .andExpect(jsonPath("$.code_challenge_methods_supported[0]").value("S256"));
    }

    @Test
    public void testIssuerAndMetadataConsistency() throws Exception {
        MvcResult asResult = mvc.perform(get("/.well-known/oauth-authorization-server"))
                .andExpect(status().isOk())
                .andReturn();
        String asIssuer = objectMapper.readTree(asResult.getResponse().getContentAsString()).get("issuer").asText();

        MvcResult prResult = mvc.perform(get("/.well-known/oauth-protected-resource/mcp"))
                .andExpect(status().isOk())
                .andReturn();
        String prmAuthServer = objectMapper.readTree(prResult.getResponse().getContentAsString()).get("authorization_servers").get(0).asText();

        assertThat(asIssuer).isEqualTo("http://localhost:8080");
        assertThat(prmAuthServer).isEqualTo(asIssuer);
    }

    @Test
    public void testDynamicClientRegistrationAndTokenSettings() throws Exception {
        Map<String, Object> registrationRequest = new HashMap<>();
        registrationRequest.put("client_name", "Claude Code AI Assistant");
        registrationRequest.put("redirect_uris", List.of("http://127.0.0.1:8123/callback"));
        registrationRequest.put("grant_types", List.of("authorization_code", "refresh_token"));
        registrationRequest.put("token_endpoint_auth_method", "none");

        MvcResult result = mvc.perform(post("/oauth2/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registrationRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.client_id").exists())
                .andExpect(jsonPath("$.client_name").value("Claude Code AI Assistant"))
                .andReturn();

        JsonNode responseNode = objectMapper.readTree(result.getResponse().getContentAsString());
        String clientId = responseNode.get("client_id").asText();

        RegisteredClient savedClient = registeredClientRepository.findByClientId(clientId);
        assertThat(savedClient).isNotNull();
        assertThat(savedClient.getTokenSettings().getAccessTokenTimeToLive()).isEqualTo(Duration.ofHours(1));
        assertThat(savedClient.getTokenSettings().getRefreshTokenTimeToLive()).isEqualTo(Duration.ofDays(30));
        assertThat(savedClient.getTokenSettings().isReuseRefreshTokens()).isFalse();
    }

    @Test
    public void testDynamicClientRegistrationRejectsUnsupportedGrantTypes() throws Exception {
        Map<String, Object> registrationRequest = new HashMap<>();
        registrationRequest.put("client_name", "Malicious Client");
        registrationRequest.put("redirect_uris", List.of("http://127.0.0.1:8123/callback"));
        registrationRequest.put("grant_types", List.of("client_credentials"));

        mvc.perform(post("/oauth2/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registrationRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid_client_metadata"));
    }

    @Test
    public void testUnauthenticatedTokenEndpointReturns401NotRedirect() throws Exception {
        mvc.perform(post("/oauth2/token")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testAuthorizeWithoutPkceRejected() throws Exception {
        // Register client
        Map<String, Object> registrationRequest = new HashMap<>();
        registrationRequest.put("client_name", "PKCE Test Client");
        registrationRequest.put("redirect_uris", List.of("http://127.0.0.1:8123/callback"));
        registrationRequest.put("grant_types", List.of("authorization_code"));
        registrationRequest.put("token_endpoint_auth_method", "none");

        MvcResult regResult = mvc.perform(post("http://localhost:8080/oauth2/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registrationRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        String clientId = objectMapper.readTree(regResult.getResponse().getContentAsString()).get("client_id").asText();

        // Create authenticated user session
        String email = "pkce-user-" + UUID.randomUUID() + "@test.com";
        String password = "Password123!";
        userTestingUtils.createTestUser(email, password);

        MockHttpSession session = new MockHttpSession();
        mvc.perform(post("http://localhost:8080/login")
                        .session(session)
                        .with(csrf())
                        .param("username", email)
                        .param("password", password))
                .andExpect(status().is3xxRedirection());

        // Authenticated request without code_challenge must fail (HTTP 302 redirect back with error=invalid_request per RFC 7636)
        String authorizeUrlWithoutPkce = String.format(
                "http://localhost:8080/oauth2/authorize?response_type=code&client_id=%s&redirect_uri=%s",
                clientId,
                "http://127.0.0.1:8123/callback"
        );

        MvcResult authResult = mvc.perform(get(authorizeUrlWithoutPkce).session(session))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        String redirectLocation = authResult.getResponse().getHeader("Location");
        assertThat(redirectLocation).startsWith("http://127.0.0.1:8123/callback");
        assertThat(redirectLocation).contains("error=invalid_request");
    }

    @Test
    public void testFullOAuthFlowWithPkceAndUserLogin() throws Exception {
        // 1. DCR: Register a public client
        Map<String, Object> registrationRequest = new HashMap<>();
        registrationRequest.put("client_name", "Test AI Client");
        registrationRequest.put("redirect_uris", List.of("http://127.0.0.1:8123/callback"));
        registrationRequest.put("grant_types", List.of("authorization_code", "refresh_token"));
        registrationRequest.put("response_types", List.of("code"));
        registrationRequest.put("scope", "mcp");
        registrationRequest.put("token_endpoint_auth_method", "none");

        MvcResult regResult = mvc.perform(post("http://localhost:8080/oauth2/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registrationRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        String clientId = objectMapper.readTree(regResult.getResponse().getContentAsString()).get("client_id").asText();

        // 2. Setup user and PKCE verifier/challenge
        String email = "oauth-user-" + UUID.randomUUID() + "@test.com";
        String password = "SecurePassword123!";
        EisenhowerUserEntity user = userTestingUtils.createTestUser(email, password);

        String codeVerifier = generateCodeVerifier();
        String codeChallenge = generateCodeChallenge(codeVerifier);

        // 3. Initiate authorization request - should redirect to /login
        MockHttpSession session = new MockHttpSession();
        String authorizeUrl = String.format(
                "http://localhost:8080/oauth2/authorize?response_type=code&client_id=%s&redirect_uri=%s&scope=%s&code_challenge=%s&code_challenge_method=S256&resource=%s",
                clientId,
                "http://127.0.0.1:8123/callback",
                "mcp",
                codeChallenge,
                "http://localhost:8080/mcp"
        );

        MvcResult authInitResult = mvc.perform(get(authorizeUrl).accept(MediaType.TEXT_HTML).session(session))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        assertThat(authInitResult.getResponse().getHeader("Location")).contains("/login");

        // 4. Authenticate user via form login
        MvcResult loginResult = mvc.perform(post("http://localhost:8080/login")
                        .session(session)
                        .with(csrf())
                        .param("username", email)
                        .param("password", password))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        // Following login redirect returns to authorization endpoint
        String redirectedUrlAfterLogin = loginResult.getResponse().getHeader("Location");

        // 5. Follow authorization with authenticated session to get code redirect
        MvcResult authCompleteResult = mvc.perform(get(redirectedUrlAfterLogin)
                        .session(session))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        String clientRedirect = authCompleteResult.getResponse().getHeader("Location");
        assertThat(clientRedirect).startsWith("http://127.0.0.1:8123/callback?code=");

        URI redirectUri = URI.create(clientRedirect);
        String authCode = extractQueryParam(redirectUri.getQuery(), "code");
        assertThat(authCode).isNotBlank();

        // 6. Exchange authorization code for tokens
        MvcResult tokenResult = mvc.perform(post("http://localhost:8080/oauth2/token")
                        .param("grant_type", "authorization_code")
                        .param("client_id", clientId)
                        .param("redirect_uri", "http://127.0.0.1:8123/callback")
                        .param("code", authCode)
                        .param("code_verifier", codeVerifier)
                        .param("resource", "http://localhost:8080/mcp"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").exists())
                .andExpect(jsonPath("$.refresh_token").exists())
                .andExpect(jsonPath("$.token_type").value("Bearer"))
                .andExpect(jsonPath("$.expires_in").isNumber())
                .andReturn();

        JsonNode tokenNode = objectMapper.readTree(tokenResult.getResponse().getContentAsString());
        String accessToken = tokenNode.get("access_token").asText();
        String refreshToken = tokenNode.get("refresh_token").asText();
        int expiresIn = tokenNode.get("expires_in").asInt();

        assertThat(expiresIn).isBetween(3500, 3600);

        // 7. Verify audience and issuer on access token
        SignedJWT parsedAccessToken = SignedJWT.parse(accessToken);
        assertThat(parsedAccessToken.getJWTClaimsSet().getAudience()).contains("http://localhost:8080/mcp");
        assertThat(parsedAccessToken.getJWTClaimsSet().getIssuer()).isEqualTo("http://localhost:8080");
        assertThat(parsedAccessToken.getJWTClaimsSet().getSubject()).isEqualTo(email);

        // 8. Test issued token against /mcp endpoint
        mvc.perform(post("http://localhost:8080/mcp")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON, MediaType.TEXT_EVENT_STREAM)
                        .content("{\"jsonrpc\":\"2.0\",\"id\":\"1\",\"method\":\"tools/list\"}"))
                .andExpect(status().isOk());

        // 9. Test refresh token sliding window (reuseRefreshTokens = false) and audience preservation
        MvcResult refreshResult = mvc.perform(post("http://localhost:8080/oauth2/token")
                        .param("grant_type", "refresh_token")
                        .param("client_id", clientId)
                        .param("refresh_token", refreshToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").exists())
                .andExpect(jsonPath("$.refresh_token").exists())
                .andReturn();

        JsonNode refreshNode = objectMapper.readTree(refreshResult.getResponse().getContentAsString());
        String refreshedAccessToken = refreshNode.get("access_token").asText();
        String newRefreshToken = refreshNode.get("refresh_token").asText();
        assertThat(newRefreshToken).isNotEqualTo(refreshToken);

        // Verify audience, issuer, and subject on refreshed access token
        SignedJWT parsedRefreshedToken = SignedJWT.parse(refreshedAccessToken);
        assertThat(parsedRefreshedToken.getJWTClaimsSet().getAudience()).contains("http://localhost:8080/mcp");
        assertThat(parsedRefreshedToken.getJWTClaimsSet().getIssuer()).isEqualTo("http://localhost:8080");
        assertThat(parsedRefreshedToken.getJWTClaimsSet().getSubject()).isEqualTo(email);

        // Verify refreshed access token works on /mcp
        mvc.perform(post("http://localhost:8080/mcp")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + refreshedAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON, MediaType.TEXT_EVENT_STREAM)
                        .content("{\"jsonrpc\":\"2.0\",\"id\":\"2\",\"method\":\"tools/list\"}"))
                .andExpect(status().isOk());

        // Reusing the old rotated refresh token must fail with 400 Bad Request (invalid_grant)
        mvc.perform(post("http://localhost:8080/oauth2/token")
                        .param("grant_type", "refresh_token")
                        .param("client_id", clientId)
                        .param("refresh_token", refreshToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid_grant"));
    }

    @Test
    public void testClientCannotRefreshAnotherClientsToken() throws Exception {
        // Register Client A
        Map<String, Object> regA = new HashMap<>();
        regA.put("client_name", "Client A");
        regA.put("redirect_uris", List.of("http://127.0.0.1:8123/callback"));
        regA.put("grant_types", List.of("authorization_code", "refresh_token"));
        regA.put("response_types", List.of("code"));
        regA.put("scope", "mcp");
        regA.put("token_endpoint_auth_method", "none");
        MvcResult regResultA = mvc.perform(post("http://localhost:8080/oauth2/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(regA)))
                .andExpect(status().isCreated())
                .andReturn();
        String clientIdA = objectMapper.readTree(regResultA.getResponse().getContentAsString()).get("client_id").asText();

        // Register Client B
        Map<String, Object> regB = new HashMap<>();
        regB.put("client_name", "Client B");
        regB.put("redirect_uris", List.of("http://127.0.0.1:8123/callback"));
        regB.put("grant_types", List.of("authorization_code", "refresh_token"));
        regB.put("response_types", List.of("code"));
        regB.put("scope", "mcp");
        regB.put("token_endpoint_auth_method", "none");
        MvcResult regResultB = mvc.perform(post("http://localhost:8080/oauth2/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(regB)))
                .andExpect(status().isCreated())
                .andReturn();
        String clientIdB = objectMapper.readTree(regResultB.getResponse().getContentAsString()).get("client_id").asText();

        // User logs in for Client A
        String email = "impersonate-user-" + UUID.randomUUID() + "@test.com";
        String password = "Password123!";
        userTestingUtils.createTestUser(email, password);
        String codeVerifier = generateCodeVerifier();
        String codeChallenge = generateCodeChallenge(codeVerifier);

        MockHttpSession session = new MockHttpSession();
        String authorizeUrl = String.format(
                "http://localhost:8080/oauth2/authorize?response_type=code&client_id=%s&redirect_uri=%s&scope=%s&code_challenge=%s&code_challenge_method=S256&resource=%s",
                clientIdA,
                "http://127.0.0.1:8123/callback",
                "mcp",
                codeChallenge,
                "http://localhost:8080/mcp"
        );
        mvc.perform(get(authorizeUrl).accept(MediaType.TEXT_HTML).session(session))
                .andExpect(status().is3xxRedirection());

        MvcResult loginResult = mvc.perform(post("http://localhost:8080/login")
                        .session(session)
                        .with(csrf())
                        .param("username", email)
                        .param("password", password))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        String redirectedUrlAfterLogin = loginResult.getResponse().getHeader("Location");

        MvcResult authCompleteResult = mvc.perform(get(redirectedUrlAfterLogin).session(session))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        String clientRedirect = authCompleteResult.getResponse().getHeader("Location");
        String authCode = extractQueryParam(URI.create(clientRedirect).getQuery(), "code");

        // Client A exchanges code for tokens
        MvcResult tokenResult = mvc.perform(post("http://localhost:8080/oauth2/token")
                        .param("grant_type", "authorization_code")
                        .param("client_id", clientIdA)
                        .param("redirect_uri", "http://127.0.0.1:8123/callback")
                        .param("code", authCode)
                        .param("code_verifier", codeVerifier)
                        .param("resource", "http://localhost:8080/mcp"))
                .andExpect(status().isOk())
                .andReturn();
        String refreshTokenA = objectMapper.readTree(tokenResult.getResponse().getContentAsString()).get("refresh_token").asText();

        // Client B attempts to use Client A's refresh token -> must fail with 400 (invalid_grant)
        mvc.perform(post("http://localhost:8080/oauth2/token")
                        .param("grant_type", "refresh_token")
                        .param("client_id", clientIdB)
                        .param("refresh_token", refreshTokenA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid_grant"));
    }

    @Test
    public void testMcpAudienceValidation() throws Exception {
        // Create token with valid audience
        String validToken = mintTestJwt(List.of("http://localhost:8080/mcp"), "test@example.com", "http://localhost:8080", Duration.ofHours(1));
        mvc.perform(post("http://localhost:8080/mcp")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON, MediaType.TEXT_EVENT_STREAM)
                        .content("{\"jsonrpc\":\"2.0\",\"id\":\"1\",\"method\":\"tools/list\"}"))
                .andExpect(status().isOk());

        // Create token with missing / empty audience
        String noAudienceToken = mintTestJwt(Collections.emptyList(), "test@example.com", "http://localhost:8080", Duration.ofHours(1));
        mvc.perform(post("http://localhost:8080/mcp")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + noAudienceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON, MediaType.TEXT_EVENT_STREAM)
                        .content("{\"jsonrpc\":\"2.0\",\"id\":\"1\",\"method\":\"tools/list\"}"))
                .andExpect(status().isUnauthorized());

        // Create token with invalid audience
        String invalidAudienceToken = mintTestJwt(List.of("http://evil.com/mcp"), "test@example.com", "http://localhost:8080", Duration.ofHours(1));
        mvc.perform(post("http://localhost:8080/mcp")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + invalidAudienceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON, MediaType.TEXT_EVENT_STREAM)
                        .content("{\"jsonrpc\":\"2.0\",\"id\":\"1\",\"method\":\"tools/list\"}"))
                .andExpect(status().isUnauthorized());

        // Create expired token
        String expiredToken = mintTestJwt(List.of("http://localhost:8080/mcp"), "test@example.com", "http://localhost:8080", Duration.ofHours(-1));
        mvc.perform(post("http://localhost:8080/mcp")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + expiredToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON, MediaType.TEXT_EVENT_STREAM)
                        .content("{\"jsonrpc\":\"2.0\",\"id\":\"1\",\"method\":\"tools/list\"}"))
                .andExpect(status().isUnauthorized());

        // Create token with invalid issuer
        String invalidIssuerToken = mintTestJwt(List.of("http://localhost:8080/mcp"), "test@example.com", "http://evil.com", Duration.ofHours(1));
        mvc.perform(post("http://localhost:8080/mcp")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + invalidIssuerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON, MediaType.TEXT_EVENT_STREAM)
                        .content("{\"jsonrpc\":\"2.0\",\"id\":\"1\",\"method\":\"tools/list\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testProductionBlankSigningKeyFailsClosed() {
        OAuthConfiguration oAuthConfiguration = new OAuthConfiguration();
        MockEnvironment prodEnv = new MockEnvironment();
        prodEnv.setProperty("spring.datasource.url", "jdbc:sqlserver://prod-db:1433;database=eisenhower");

        assertThatThrownBy(() -> oAuthConfiguration.jwkSource("", prodEnv))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("eisenhower.oauth.signing-key must be configured in production");
    }

    @Test
    public void testPkcs8SigningKeyParsing() throws Exception {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        KeyPair pair = gen.generateKeyPair();
        String pem = "-----BEGIN PRIVATE KEY-----\n" +
                Base64.getMimeEncoder().encodeToString(pair.getPrivate().getEncoded()) +
                "\n-----END PRIVATE KEY-----";

        OAuthConfiguration oAuthConfiguration = new OAuthConfiguration();
        MockEnvironment prodEnv = new MockEnvironment();
        prodEnv.setProperty("spring.datasource.url", "jdbc:sqlserver://prod-db:1433;database=eisenhower");

        JWKSource<SecurityContext> source = oAuthConfiguration.jwkSource(pem, prodEnv);
        List<JWK> jwks = source.get(new JWKSelector(new JWKMatcher.Builder().build()), null);
        assertThat(jwks).isNotEmpty();
        assertThat(jwks.get(0)).isInstanceOf(RSAKey.class);
    }

    private String mintTestJwt(List<String> audience, String subject, String issuer, Duration validity) throws Exception {
        List<JWK> jwks = jwkSource.get(new JWKSelector(new JWKMatcher.Builder().build()), null);
        RSAKey rsaKey = (RSAKey) jwks.get(0);
        RSAPrivateKey privateKey = rsaKey.toRSAPrivateKey();

        Instant now = Instant.now();
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject(subject)
                .issuer(issuer)
                .audience(audience)
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plus(validity)))
                .build();

        SignedJWT signedJWT = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(rsaKey.getKeyID()).build(),
                claimsSet
        );
        signedJWT.sign(new RSASSASigner(privateKey));
        return signedJWT.serialize();
    }

    private static String generateCodeVerifier() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String generateCodeChallenge(String codeVerifier) throws Exception {
        byte[] bytes = codeVerifier.getBytes(StandardCharsets.US_ASCII);
        MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
        messageDigest.update(bytes);
        byte[] digest = messageDigest.digest();
        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
    }

    private static String extractQueryParam(String query, String name) {
        for (String param : query.split("&")) {
            String[] pair = param.split("=");
            if (pair.length == 2 && pair[0].equals(name)) {
                return URLDecoder.decode(pair[1], StandardCharsets.UTF_8);
            }
        }
        return null;
    }
}
