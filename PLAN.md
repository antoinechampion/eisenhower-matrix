# Plan: MCP server for Eisenhower Matrix (Spring AI + MCP OAuth)

## Context
We want AI clients (Claude Code, Claude Desktop, claude.ai, Perplexity, …) to read a user's notes through a remote MCP server at `https://<app-host>/mcp`. It exposes two read-only tools:
- `list_notes`: all of the user's notes, across all boards, ordered by priority.
- `get_note`: details of one note, including its attached document.

Auth follows the MCP authorization spec:
- OAuth 2.1 + PKCE.
- RFC 9728 protected-resource discovery.
- RFC 7591 Dynamic Client Registration (DCR).
- RFC 8707 audience-bound tokens.
- Refresh tokens.

User experience: the user adds `https://<app-host>/mcp` in their client and logs in with their existing Eisenhower email/password in a browser. The client then refreshes tokens silently.

### Decisions already made (do not revisit)
| Topic | Decision | Why |
|---|---|---|
| MCP framework | Spring AI MCP server (`spring-ai-starter-mcp-server-webmvc`) | Most "native" option: tools are annotated methods |
| Spring Boot | Upgrade 3.0.0 → **3.5.16** | Spring AI 1.1 needs Boot ≥ 3.4 |
| Spring AI | **1.1.8** (BOM) | Last line compatible with Boot 3.5 (2.x needs Boot 4) |
| MCP auth helpers | `org.springaicommunity:mcp-server-security` + `mcp-authorization-server` **0.0.6** | 0.1.x requires Boot 4 / Security 7; 0.0.6 targets Security 6.5 and already provides open DCR, protected-resource metadata, audience claims |
| Auth server | Spring Authorization Server, embedded in this app | Reuses the existing user table & password check |
| OAuth storage | Spring AS **JDBC** stores + a hand-written SQL schema | Spring AS has no JPA entities; `ddl-auto` won't create these tables |
| Signing key | RSA key from new env var `OAUTH_SIGNING_KEY` | Persisted tokens must survive restarts |
| Login page | Spring Security's default `/login` page | Simplest; branding is a follow-up |

### Library facts verified from the 0.0.6 sources (so you don't have to)
- DCR endpoint is `POST /oauth2/register`. It is open (no initial access token) and CSRF-exempt. Registered clients get `requireProofKey=true` and `requireAuthorizationConsent=false`, so there's no consent page.
- The registration converter also adds the `client_secret_basic`, `client_secret_post` and `none` auth methods to every client. Claude Code uses `client_secret_post` + PKCE.
- The protected-resource metadata filter serves `GET /.well-known/oauth-protected-resource/**`. The 401 entry point adds `resource_metadata=<scheme://host>/.well-known/oauth-protected-resource/mcp`.
- Both build URLs from the incoming request URL, so behind the nginx ingress we **must** honor `X-Forwarded-*` headers (see step 3).
- By default, `McpServerOAuth2Configurer` builds a JWT decoder that fetches the issuer metadata over HTTP: the app would call its own public URL. We override it with a local decoder (step 5).

---

## Step 1: Upgrade Spring Boot 3.0 → 3.5 (separate commit)
Do this alone first and make sure the app still works before adding anything.

`pom.xml`:
1. `<parent>` version `3.0.0` → `3.5.16`.
2. **Remove every `<version>3.0.1</version>`** on the `spring-boot-starter-*` and `spring-boot-devtools` dependencies. Mixing 3.0.1 starters with a 3.5 parent breaks the build. The parent manages these versions.
3. `springdoc-openapi-starter-webmvc-ui` `2.0.2` → `2.8.17`.
4. Leave `jjwt` (0.11.5) and `mssql-jdbc` (11.2.2.jre17) as they are.

`src/main/resources/application.properties`, add:
```properties
# Hibernate 6.2+ maps UUID to SQL Server 'uniqueidentifier'; existing columns are binary. Keep the old mapping.
spring.jpa.properties.hibernate.type.preferred_uuid_jdbc_type=BINARY
```

Do **not** refactor the existing `SecurityConfiguration.filterChain` (`.httpBasic().disable()` etc.). It's deprecated in Security 6.5 but still compiles; leave it.

**Checkpoint 1**
- `mvn -DskipTests package` compiles. Fix any compile errors caused by the upgrade without changing behavior.
- `mvn test`: all existing tests pass.
- **UUID regression check against real SQL Server.** Prod is SQL Server, local/test are H2, so this is the only way to catch the UUID issue:
  ```bash
  docker run -d --name mssql -e ACCEPT_EULA=Y -e MSSQL_SA_PASSWORD='Passw0rd!123' -p 1433:1433 mcr.microsoft.com/mssql/server:2022-latest
  # create DB 'eisenhower' (e.g. with sqlcmd inside the container), then:
  export JDBC_URL='jdbc:sqlserver://localhost:1433;databaseName=eisenhower;encrypt=false' DATABASE_USER=sa DATABASE_PASSWORD='Passw0rd!123' JWT_SIGNING_KEY=some-long-dev-key-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx APP_BASE_URL=http://localhost:8080 MAIL_SMTP_HOST=localhost MAIL_SMTP_PORT=1025 MAIL_SMTP_USER= MAIL_SMTP_PASSWORD= MAIL_FROM=test@localhost
  ```
  1. Check out the **old** commit (`git stash`/`git worktree`). Run it with the default profile, then register, create a board and a few notes.
  2. Stop it, switch to the upgraded code, and run it on the **same DB**. Log in: the existing notes must load, move and save.
- Commit: `chore(deps): upgrade spring boot to 3.5`.

---

## Step 2: Dependencies (`pom.xml`)
Add a `<dependencyManagement>` block (there is none today):
```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.ai</groupId>
            <artifactId>spring-ai-bom</artifactId>
            <version>1.1.8</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```
Add the dependencies:
```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-mcp-server-webmvc</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-authorization-server</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
</dependency>
<dependency>
    <groupId>org.springaicommunity</groupId>
    <artifactId>mcp-server-security</artifactId>
    <version>0.0.6</version>
</dependency>
<dependency>
    <groupId>org.springaicommunity</groupId>
    <artifactId>mcp-authorization-server</artifactId>
    <version>0.0.6</version>
</dependency>
```

---

## Step 3: Properties

`application.properties` (prod), add:
```properties
spring.ai.mcp.server.name=eisenhower-matrix
# Stateless streamable HTTP: no MCP sessions; tools run on the request thread, so SecurityContextHolder works inside tools.
# The endpoint defaults to /mcp.
spring.ai.mcp.server.protocol=STATELESS

# Behind the nginx ingress: build https:// URLs (issuer checks, resource metadata, redirects) from X-Forwarded-* headers
server.forward-headers-strategy=framework

# OAuth tables (see step 4)
spring.sql.init.mode=always
spring.sql.init.schema-locations=classpath:oauth2/schema-sqlserver.sql

eisenhower.oauth.signing-key=${OAUTH_SIGNING_KEY}
```

`application-local.properties` **and** `application-test.properties`, add:
```properties
spring.sql.init.schema-locations=classpath:oauth2/schema-h2.sql
# Blank = generate a throwaway RSA key at startup
eisenhower.oauth.signing-key=
```

---

## Step 4: OAuth SQL schema (two new files)
Spring AS ships its DDL inside its jar. Find the files:
```bash
unzip -l ~/.m2/repository/org/springframework/security/spring-security-oauth2-authorization-server/1.5.*/spring-security-oauth2-authorization-server-1.5.*.jar | grep schema.sql
```
We need two of them:
- `.../client/oauth2-registered-client-schema.sql` (table `oauth2_registered_client`)
- `.../oauth2-authorization-schema.sql` (table `oauth2_authorization`)

The consent table is **not** needed: DCR clients never require consent, and the default in-memory consent service is fine.

1. **`src/main/resources/oauth2/schema-h2.sql`:** copy both `CREATE TABLE` statements verbatim and change `CREATE TABLE` → `CREATE TABLE IF NOT EXISTS`.
2. **`src/main/resources/oauth2/schema-sqlserver.sql`:** the same two tables, adapted for SQL Server:
   - Wrap each statement: `IF OBJECT_ID(N'oauth2_registered_client', N'U') IS NULL CREATE TABLE oauth2_registered_client (...);` Keep each statement on one logical statement ending with `;`; Spring splits scripts on `;`.
   - `blob` → `varbinary(max)`.
   - `timestamp` → `datetime2`. **Important:** in SQL Server, `timestamp` means `rowversion`, not a date.
   - Keep `DEFAULT CURRENT_TIMESTAMP NOT NULL` on `client_id_issued_at` as is (valid with `datetime2`).
   - Keep the `varchar(n)` columns unchanged.

Both scripts must be idempotent, because they run at every startup.

---

## Step 5: Security & authorization server configuration

### 5a. New class `src/main/java/com/ach/eisenhower/configuration/OAuthConfiguration.java`
`@Configuration`. It holds the authorization server beans:

```java
/** Signing key for MCP OAuth access tokens. Loaded from PEM (PKCS#8) or generated when blank (local/test). */
@Bean
JWKSource<SecurityContext> jwkSource(@Value("${eisenhower.oauth.signing-key:}") String pem) throws Exception {
    RSAPrivateCrtKey privateKey;
    if (pem.isBlank()) {
        // log a warning: "No OAUTH_SIGNING_KEY set, generating a temporary key"
        var generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        privateKey = (RSAPrivateCrtKey) generator.generateKeyPair().getPrivate();
    } else {
        privateKey = (RSAPrivateCrtKey) RsaKeyConverters.pkcs8()
                .convert(new ByteArrayInputStream(pem.getBytes(StandardCharsets.UTF_8)));
    }
    var publicKey = (RSAPublicKey) KeyFactory.getInstance("RSA")
            .generatePublic(new RSAPublicKeySpec(privateKey.getModulus(), privateKey.getPublicExponent()));
    var rsaKey = new RSAKey.Builder(publicKey).privateKey(privateKey).keyID("eisenhower-oauth").build();
    return new ImmutableJWKSet<>(new JWKSet(rsaKey));
}

@Bean
AuthorizationServerSettings authorizationServerSettings(@Value("${eisenhower.app.base-url}") String baseUrl) {
    return AuthorizationServerSettings.builder().issuer(baseUrl).build();
}

/**
 * JDBC client store. Every client registered through DCR gets refresh tokens and our token lifetimes
 * (Spring defaults are 5 min access / 1 h refresh, which would force frequent re-logins).
 */
@Bean
RegisteredClientRepository registeredClientRepository(JdbcTemplate jdbcTemplate) {
    var delegate = new JdbcRegisteredClientRepository(jdbcTemplate);
    return new RegisteredClientRepository() {
        @Override
        public void save(RegisteredClient client) {
            delegate.save(RegisteredClient.from(client)
                    .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                    .tokenSettings(TokenSettings.builder()
                            .accessTokenTimeToLive(Duration.ofHours(1))
                            .refreshTokenTimeToLive(Duration.ofDays(30))
                            .reuseRefreshTokens(false) // rotate => 30-day sliding window
                            .build())
                    .build());
        }
        @Override public RegisteredClient findById(String id) { return delegate.findById(id); }
        @Override public RegisteredClient findByClientId(String clientId) { return delegate.findByClientId(clientId); }
    };
}

@Bean
OAuth2AuthorizationService authorizationService(JdbcTemplate jdbcTemplate, RegisteredClientRepository clients) {
    return new JdbcOAuth2AuthorizationService(jdbcTemplate, clients);
}
```
Imports to note:
- `com.nimbusds.jose.jwk.*`
- `com.nimbusds.jose.jwk.source.*`
- `com.nimbusds.jose.proc.SecurityContext`
- `org.springframework.security.converter.RsaKeyConverters`
- `org.springframework.security.oauth2.server.authorization.*`
- `...authorization.client.*`
- `...authorization.settings.*`

### 5b. `SecurityConfiguration.java`: add two chains, order the existing one last
Add `@Order(3)` to the existing `filterChain` bean and change **nothing else** in it.

Add:
```java
/** OAuth2 authorization server for MCP clients: DCR, authorize, token, JWKS, metadata + login page. */
@Bean
@Order(1)
SecurityFilterChain authorizationServerFilterChain(HttpSecurity http) throws Exception {
    return http
            .securityMatcher("/oauth2/**", "/.well-known/oauth-authorization-server/**",
                    "/.well-known/openid-configuration/**", "/login")
            .with(McpAuthorizationServerConfigurer.mcpAuthorizationServer(), Customizer.withDefaults())
            .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
            // Default login page; credentials are checked by the existing customAuthenticationManager bean
            .formLogin(form -> form.permitAll())
            .build();
}

/** MCP endpoint: OAuth2 resource server accepting tokens issued by the chain above. */
@Bean
@Order(2)
SecurityFilterChain mcpFilterChain(HttpSecurity http, JWKSource<SecurityContext> jwkSource,
                                   AuthorizationServerSettings settings) throws Exception {
    // Local decoder (no HTTP call to our own issuer URL) that checks signature, expiry, issuer and audience
    var decoder = (NimbusJwtDecoder) OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
    decoder.setJwtValidator(JwtValidators.createDefaultWithValidators(
            new JwtIssuerValidator(settings.getIssuer()),
            new JwtResourceValidator(new ResourceIdentifier("/mcp"))));

    return http
            .securityMatcher("/mcp", "/mcp/**", "/.well-known/oauth-protected-resource/**")
            .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
            .with(McpServerOAuth2Configurer.mcpServerOAuth2(), mcp -> mcp
                    .authorizationServer(settings.getIssuer())
                    .resourcePath("/mcp")
                    .resourceName("Eisenhower Matrix")
                    .oauth2ResourceServer(rs -> rs.jwt(jwt -> jwt.decoder(decoder))))
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .build();
}
```

Notes for the implementer:
- **How login works:** `formLogin` authenticates through the chain's parent `AuthenticationManager`. That's the existing `customAuthenticationManager` bean: same email/password check, same "disabled" rule. Its principal is the email string, so the JWT `sub` is the user's email.
  - If login fails with "No AuthenticationProvider found", add a `UserDetailsService` bean backed by `EisenhowerUserRepository.findByEmail` instead.
  - Do **not** call `http.authenticationManager(...)` on chain 1: it would disable the authorization server's client-authentication providers.
- `/login` doesn't conflict with the SPA forward (`/{word}` → `index.html` in `WebAppConfiguration`). Chain 1 handles it before MVC.
- Chain 1 uses an HTTP session (the default), needed to remember the authorize request across the login redirect. Chains 2 and 3 stay stateless.
- Don't touch `UnauthenticatedRequestHandler`; it only applies to chain 3.

---

## Step 6: MCP tools

### 6a. Repository & service
- `NoteRepository`: add `List<NoteEntity> findAllByBoardUserId(UUID userId);` This is a Spring Data derived query on `board.user.id`.
- `NoteService`: add
  ```java
  public List<NoteEntity> listUserNotes(UUID userId) {
      return noteRepository.findAllByBoardUserId(userId);
  }
  ```

### 6b. New class `src/main/java/com/ach/eisenhower/mcp/NoteMcpTools.java`
`@Component`. Constructor-injects:
- `NoteService`
- `AttachedDocumentRepository`
- `EisenhowerUserRepository`

The annotations come from the `org.springaicommunity.mcp.annotation` package (`@McpTool`, `@McpToolParam`), pulled in by the Spring AI 1.1 starter. Spring AI scans `@Component` beans for them automatically.

**Priority semantics.** Stored values are screen coordinates (see `NoteCard.vue`: `x = urgency * width`, `y = importance * height`). `BackgroundCanvas.vue` puts IMPORTANT/URGENT at the top-left. So a **lower** stored value means **more** important or urgent. We expose inverted scores:
```java
public enum Quadrant { DO, SCHEDULE, DELEGATE, ELIMINATE } // declaration order = priority order

static double importanceScore(NoteEntity n) { return 1 - n.getImportance(); } // 1 = most important
static double urgencyScore(NoteEntity n)    { return 1 - n.getUrgency(); }    // 1 = most urgent

static Quadrant quadrant(NoteEntity n) {
    boolean important = importanceScore(n) > 0.5;
    boolean urgent = urgencyScore(n) > 0.5;
    if (important) return urgent ? Quadrant.DO : Quadrant.SCHEDULE;
    return urgent ? Quadrant.DELEGATE : Quadrant.ELIMINATE;
}

/** Quadrant first, then distance from the "least important & urgent" corner, descending. */
static final Comparator<NoteEntity> BY_PRIORITY = Comparator
        .comparing(NoteMcpTools::quadrant)
        .thenComparing(n -> Math.hypot(importanceScore(n), urgencyScore(n)), Comparator.reverseOrder());
```
The frontend's "Flip Matrix" setting only swaps labels in the browser (localStorage) and never changes data. Ignore it.

**DTOs** (records in the same package):
```java
public record NoteSummary(UUID id, String text, Quadrant quadrant, double importance, double urgency, String board) {
    static NoteSummary from(NoteEntity n) {
        return new NoteSummary(n.getId(), n.getText(), quadrant(n), importanceScore(n), urgencyScore(n), n.getBoard().getTitle());
    }
}
public record NoteDetails(UUID id, String text, Quadrant quadrant, double importance, double urgency, String board, String document) { ... }
```

**Tools:**
```java
@McpTool(name = "list_notes", description = """
        Lists all the user's Eisenhower matrix notes across all boards, most important first.
        Sorted by quadrant (DO = important & urgent, SCHEDULE = important only, DELEGATE = urgent only,
        ELIMINATE = neither), then by combined importance/urgency. Scores are in [0, 1], 1 being the highest.""")
public List<NoteSummary> listNotes() {
    return noteService.listUserNotes(currentUserId()).stream()
            .sorted(BY_PRIORITY).map(NoteSummary::from).toList();
}

@McpTool(name = "get_note", description = "Gets a note's details, including its attached document (HTML) if any.")
public NoteDetails getNote(@McpToolParam(description = "Note id, as returned by list_notes", required = true) String noteId) {
    var note = noteService.getNote(currentUserId(), UUID.fromString(noteId)); // existing ownership check
    var doc = attachedDocumentRepository.findByNoteId(note.getId());          // null when no document
    return NoteDetails.from(note, doc == null ? null : new String(doc.getContent(), StandardCharsets.UTF_8));
}

private UUID currentUserId() {
    var email = SecurityContextHolder.getContext().getAuthentication().getName(); // JWT 'sub'
    return userRepository.findByEmail(email)
            .orElseThrow(() -> new EisenhowerServiceException(HttpStatus.FORBIDDEN))
            .getId();
}
```
Exceptions thrown from a tool (not found, not owner, bad UUID) are returned to the client as tool errors by Spring AI. No extra handling is needed.

**Checkpoint 2**
Run `mvn spring-boot:run -Dspring-boot.run.profiles=local`. It should start without errors, and the existing web app should still work: register, log in, notes.

---

## Step 7: Tests (`src/test/java/com/ach/eisenhower/McpTest.java`, extends `EisenhowerBaseTest`)
Minting a valid MCP token in tests (no browser flow): autowire `JWKSource<SecurityContext>` and `AuthorizationServerSettings`, then:
```java
private String mcpToken(String email, String audience) {
    var now = Instant.now();
    var claims = JwtClaimsSet.builder().issuer(settings.getIssuer()).subject(email)
            .audience(List.of(audience)).issuedAt(now).expiresAt(now.plusSeconds(300)).build();
    return new NimbusJwtEncoder(jwkSource)
            .encode(JwtEncoderParameters.from(JwsHeader.with(SignatureAlgorithm.RS256).build(), claims))
            .getTokenValue();
}
```
MockMvc's host is `http://localhost`, so a valid audience is `"http://localhost/mcp"`.

Calling a tool (stateless MCP needs no `initialize`):
```java
mvc.perform(post("/mcp")
        .header("Authorization", "Bearer " + token)
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON, MediaType.TEXT_EVENT_STREAM)
        .content("""
            {"jsonrpc":"2.0","id":1,"method":"tools/call","params":{"name":"list_notes","arguments":{}}}"""))
```
The tool's JSON output is in `result.content[0].text`.

Test cases:
1. `POST /mcp` without a token → 401, and `WWW-Authenticate` contains `resource_metadata=http://localhost/.well-known/oauth-protected-resource/mcp`.
2. `GET /.well-known/oauth-protected-resource/mcp` → 200; JSON `authorization_servers` contains the issuer.
3. `GET /.well-known/oauth-authorization-server` → 200; JSON has `registration_endpoint`, `authorization_endpoint`, `token_endpoint`.
4. `POST /oauth2/register` with `{"client_name":"test","redirect_uris":["http://localhost:6274/oauth/callback"],"grant_types":["authorization_code","refresh_token"],"token_endpoint_auth_method":"client_secret_post"}` → 201 with a `client_id`.
5. **Ordering.** Create a user and a board via `UserTestingUtil` / `BoardService.createBoard`, then notes via `NoteService.createNote(userId, boardId, text, importance, urgency)`. Remember these are **stored** values (low = important/urgent):
   - A: (0.1, 0.1) → DO, highest
   - B: (0.4, 0.4) → DO, lower than A
   - C: (0.2, 0.9) → SCHEDULE
   - D: (0.9, 0.2) → DELEGATE
   - E: (0.9, 0.9) → ELIMINATE

   Also create a note for a second user. `list_notes` must return exactly A, B, C, D, E in that order, and must not include the other user's note.
6. `get_note` on the other user's note → the response is a tool error (`result.isError == true`) or a JSON-RPC `error`.
7. A token with the wrong audience (`"http://evil/mcp"`) → 401.

**Checkpoint 3:** `mvn test` passes: new tests plus all existing ones.

---

## Step 8: Deployment
1. `k8s.yaml`: add to the container `env`, next to `JWT_SIGNING_KEY`:
   ```yaml
   - name: OAUTH_SIGNING_KEY
     valueFrom:
       secretKeyRef:
         name: eisenhower
         key: oauthSigningKey
   ```
2. **Memory:** the container runs with `-Xmx64m -XX:MaxRAM=150M`, and Spring AI + the authorization server add classes and heap. After deploying, watch for `OutOfMemoryError` or restarts. If they happen, raise them to e.g. `-Xmx96m` / `MaxRAM=200M`.
3. **Manual, done by the repo owner (not the implementer):** create the key and add it to the existing secret before deploying:
   ```bash
   openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out /tmp/oauth-signing-key.pem
   kubectl get secret eisenhower -o json \
     | jq --arg k "$(base64 < /tmp/oauth-signing-key.pem | tr -d '\n')" '.data.oauthSigningKey=$k' \
     | kubectl apply -f -
   rm /tmp/oauth-signing-key.pem
   ```
   The CI workflow doesn't inject env vars, so it needs no change.

---

## Known limitations (accepted for v1)
- **Public clients:** a client registered with `token_endpoint_auth_method=none` gets no refresh token (Spring AS rule), so it must re-login when the 1 h access token expires. Claude Code registers with `client_secret_post`, so it's unaffected.
- The login page is Spring's unbranded default page.
- There's no UI showing users the MCP URL yet. Follow-up: add it to the settings menu or FAQ.
- The tools are read-only.

## Files touched
- `pom.xml`
- `src/main/resources/application.properties`, `application-local.properties`, `application-test.properties`
- `src/main/resources/oauth2/schema-h2.sql`, `schema-sqlserver.sql` (new)
- `src/main/java/com/ach/eisenhower/configuration/OAuthConfiguration.java` (new)
- `src/main/java/com/ach/eisenhower/configuration/SecurityConfiguration.java`
- `src/main/java/com/ach/eisenhower/mcp/NoteMcpTools.java` + DTO records (new)
- `src/main/java/com/ach/eisenhower/services/NoteService.java`
- `src/main/java/com/ach/eisenhower/repositories/NoteRepository.java`
- `src/test/java/com/ach/eisenhower/McpTest.java` (new)
- `k8s.yaml`

## Final end-to-end verification
1. Start the app with the local profile: `mvn spring-boot:run -Dspring-boot.run.profiles=local`.
2. Run `curl -i -X POST localhost:8080/mcp` → 401 with a `WWW-Authenticate: Bearer resource_metadata=...` header.
3. Run `npx @modelcontextprotocol/inspector` and connect to `http://localhost:8080/mcp` (Streamable HTTP).
   - The OAuth flow should run: DCR → browser `/login` with a web-app account → back to the inspector.
   - Call `list_notes` and `get_note` and check the order matches the board on screen.
4. Run `claude mcp add --transport http eisenhower http://localhost:8080/mcp`. In Claude Code, run `/mcp` → authenticate, then ask "list my notes".
5. Refresh: temporarily set `accessTokenTimeToLive` to 1 minute, wait, and call a tool again. It should work with no re-login.
6. Restart: run against the Docker SQL Server from checkpoint 1 with a fixed `OAUTH_SIGNING_KEY`, restart the app, and call a tool. It should work with no re-login (clients and tokens are persisted).
