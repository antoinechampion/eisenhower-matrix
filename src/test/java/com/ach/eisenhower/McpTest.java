package com.ach.eisenhower;

import com.ach.eisenhower.entities.AttachedDocumentEntity;
import com.ach.eisenhower.entities.EisenhowerUserEntity;
import com.ach.eisenhower.entities.NoteEntity;
import com.ach.eisenhower.mcp.NoteDetails;
import com.ach.eisenhower.mcp.NoteSummary;
import com.ach.eisenhower.mcp.Quadrant;
import com.ach.eisenhower.repositories.AttachedDocumentRepository;
import com.ach.eisenhower.repositories.NoteRepository;
import com.ach.eisenhower.services.BoardService;
import com.ach.eisenhower.services.NoteService;
import com.fasterxml.jackson.core.type.TypeReference;
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
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.security.interfaces.RSAPrivateKey;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class McpTest extends EisenhowerBaseTest {

    @Autowired
    private JWKSource<SecurityContext> jwkSource;

    @Autowired
    private NoteRepository noteRepository;

    @Autowired
    private BoardService boardService;

    @Autowired
    private NoteService noteService;

    @Autowired
    private AttachedDocumentRepository attachedDocumentRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String MCP_ENDPOINT = "http://localhost:8080/mcp";
    private static final String ISSUER = "http://localhost:8080";
    private static final Offset<Double> DOUBLE_TOLERANCE = Offset.offset(0.001);

    // ---------------------------------------------------------------------------------------------
    // Helper Methods
    // ---------------------------------------------------------------------------------------------

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

    private String mintValidMcpToken(String email) throws Exception {
        return mintTestJwt(List.of(MCP_ENDPOINT), email, ISSUER, Duration.ofHours(1));
    }

    private JsonNode performMcpJsonRpc(String token, String requestJson) throws Exception {
        MvcResult result = mvc.perform(post(MCP_ENDPOINT)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON, MediaType.TEXT_EVENT_STREAM)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    // ---------------------------------------------------------------------------------------------
    // Discovery & Dynamic Client Registration Tests
    // ---------------------------------------------------------------------------------------------

    @Test
    public void testUnauthenticatedChallengeReturns401WithResourceMetadata() throws Exception {
        mvc.perform(post(MCP_ENDPOINT))
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
                .andExpect(jsonPath("$.authorization_servers[0]").value(ISSUER))
                .andExpect(jsonPath("$.bearer_methods_supported[0]").value("header"));
    }

    @Test
    public void testAuthorizationServerMetadataEndpoint() throws Exception {
        mvc.perform(get("/.well-known/oauth-authorization-server"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.issuer").value(ISSUER))
                .andExpect(jsonPath("$.authorization_endpoint").exists())
                .andExpect(jsonPath("$.token_endpoint").exists())
                .andExpect(jsonPath("$.jwks_uri").exists())
                .andExpect(jsonPath("$.registration_endpoint").exists())
                .andExpect(jsonPath("$.code_challenge_methods_supported[0]").value("S256"));
    }

    @Test
    public void testDynamicClientRegistration() throws Exception {
        Map<String, Object> registrationRequest = new HashMap<>();
        registrationRequest.put("client_name", "Claude Code AI Client");
        registrationRequest.put("redirect_uris", List.of("http://127.0.0.1:8085/callback"));
        registrationRequest.put("grant_types", List.of("authorization_code", "refresh_token"));
        registrationRequest.put("token_endpoint_auth_method", "none");

        mvc.perform(post("/oauth2/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registrationRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.client_id").exists())
                .andExpect(jsonPath("$.client_name").value("Claude Code AI Client"))
                .andExpect(jsonPath("$.redirect_uris[0]").value("http://127.0.0.1:8085/callback"));
    }

    // ---------------------------------------------------------------------------------------------
    // Audience, Issuer, and Expiry Validation Tests
    // ---------------------------------------------------------------------------------------------

    @Test
    public void testAudienceAndExpiryValidation() throws Exception {
        EisenhowerUserEntity user = userTestingUtils.getValidUser();

        // 1. Valid token succeeds
        String validToken = mintValidMcpToken(user.getEmail());
        mvc.perform(post(MCP_ENDPOINT)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON, MediaType.TEXT_EVENT_STREAM)
                        .content("{\"jsonrpc\":\"2.0\",\"id\":\"1\",\"method\":\"tools/list\"}"))
                .andExpect(status().isOk());

        // 2. Missing audience fails with 401
        String noAudienceToken = mintTestJwt(Collections.emptyList(), user.getEmail(), ISSUER, Duration.ofHours(1));
        mvc.perform(post(MCP_ENDPOINT)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + noAudienceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON, MediaType.TEXT_EVENT_STREAM)
                        .content("{\"jsonrpc\":\"2.0\",\"id\":\"1\",\"method\":\"tools/list\"}"))
                .andExpect(status().isUnauthorized());

        // 3. Invalid audience fails with 401
        String invalidAudienceToken = mintTestJwt(List.of("http://evil.com/mcp"), user.getEmail(), ISSUER, Duration.ofHours(1));
        mvc.perform(post(MCP_ENDPOINT)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + invalidAudienceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON, MediaType.TEXT_EVENT_STREAM)
                        .content("{\"jsonrpc\":\"2.0\",\"id\":\"1\",\"method\":\"tools/list\"}"))
                .andExpect(status().isUnauthorized());

        // 4. Expired token fails with 401
        String expiredToken = mintTestJwt(List.of(MCP_ENDPOINT), user.getEmail(), ISSUER, Duration.ofHours(-1));
        mvc.perform(post(MCP_ENDPOINT)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + expiredToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON, MediaType.TEXT_EVENT_STREAM)
                        .content("{\"jsonrpc\":\"2.0\",\"id\":\"1\",\"method\":\"tools/list\"}"))
                .andExpect(status().isUnauthorized());

        // 5. Invalid issuer fails with 401
        String invalidIssuerToken = mintTestJwt(List.of(MCP_ENDPOINT), user.getEmail(), "http://evil.com", Duration.ofHours(1));
        mvc.perform(post(MCP_ENDPOINT)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + invalidIssuerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON, MediaType.TEXT_EVENT_STREAM)
                        .content("{\"jsonrpc\":\"2.0\",\"id\":\"1\",\"method\":\"tools/list\"}"))
                .andExpect(status().isUnauthorized());
    }

    // ---------------------------------------------------------------------------------------------
    // MCP tools/list Integration Tests
    // ---------------------------------------------------------------------------------------------

    @Test
    public void testToolsListEndpointAndAccurateMetadataAnnotations() throws Exception {
        EisenhowerUserEntity user = userTestingUtils.getValidUser();
        String token = mintValidMcpToken(user.getEmail());

        String listToolsRequest = "{\"jsonrpc\":\"2.0\",\"id\":\"list-1\",\"method\":\"tools/list\"}";
        JsonNode response = performMcpJsonRpc(token, listToolsRequest);

        assertThat(response.get("jsonrpc").asText()).isEqualTo("2.0");
        assertThat(response.get("id").asText()).isEqualTo("list-1");
        assertThat(response.has("result")).isTrue();

        JsonNode tools = response.get("result").get("tools");
        assertThat(tools).isNotNull();
        assertThat(tools.isArray()).isTrue();
        assertThat(tools.size()).isEqualTo(2);

        Map<String, JsonNode> toolMap = new HashMap<>();
        for (JsonNode toolNode : tools) {
            toolMap.put(toolNode.get("name").asText(), toolNode);
        }

        assertThat(toolMap).containsKey("list_notes");
        assertThat(toolMap).containsKey("get_note");

        // Verify list_notes metadata and annotations
        JsonNode listNotesTool = toolMap.get("list_notes");
        assertThat(listNotesTool.get("description").asText()).contains("Lists all notes");
        JsonNode listNotesAnnotations = listNotesTool.get("annotations");
        assertThat(listNotesAnnotations.get("readOnlyHint").asBoolean()).isTrue();
        assertThat(listNotesAnnotations.get("destructiveHint").asBoolean()).isFalse();
        assertThat(listNotesAnnotations.get("idempotentHint").asBoolean()).isTrue();
        assertThat(listNotesAnnotations.get("openWorldHint").asBoolean()).isFalse();

        // Verify get_note metadata, inputSchema, and annotations
        JsonNode getNoteTool = toolMap.get("get_note");
        assertThat(getNoteTool.get("description").asText()).contains("Retrieves details");
        JsonNode getNoteAnnotations = getNoteTool.get("annotations");
        assertThat(getNoteAnnotations.get("readOnlyHint").asBoolean()).isTrue();
        assertThat(getNoteAnnotations.get("destructiveHint").asBoolean()).isFalse();
        assertThat(getNoteAnnotations.get("idempotentHint").asBoolean()).isTrue();
        assertThat(getNoteAnnotations.get("openWorldHint").asBoolean()).isFalse();

        JsonNode inputSchema = getNoteTool.get("inputSchema");
        assertThat(inputSchema.get("type").asText()).isEqualTo("object");
        assertThat(inputSchema.get("properties").has("id")).isTrue();
        assertThat(inputSchema.get("required").toString()).contains("id");
    }

    // ---------------------------------------------------------------------------------------------
    // MCP tools/call for list_notes Integration Tests
    // ---------------------------------------------------------------------------------------------

    @Test
    public void testToolsCallListNotesPriorityAndQuadrantOrdering() throws Exception {
        EisenhowerUserEntity user = userTestingUtils.getValidUser();
        var board = boardService.createBoard(user, "Work Matrix");

        // UI coordinates (where 0.0 is top/left = highest priority):
        // Importance raw -> score = 1.0 - raw. Urgency raw -> score = 1.0 - raw.
        // DO: importance > 0.5, urgency > 0.5 -> raw < 0.5, raw < 0.5
        // SCHEDULE: importance > 0.5, urgency <= 0.5 -> raw < 0.5, raw >= 0.5
        // DELEGATE: importance <= 0.5, urgency > 0.5 -> raw >= 0.5, raw < 0.5
        // ELIMINATE: importance <= 0.5, urgency <= 0.5 -> raw >= 0.5, raw >= 0.5

        noteService.createNote(user.getId(), board.getId(), "ELIMINATE Sort junk mail", 0.8, 0.8); // score 0.2, 0.2
        noteService.createNote(user.getId(), board.getId(), "DELEGATE Book flight", 0.8, 0.1);     // score 0.2, 0.9
        noteService.createNote(user.getId(), board.getId(), "SCHEDULE Project Plan", 0.1, 0.8);   // score 0.9, 0.2
        noteService.createNote(user.getId(), board.getId(), "DO Less Urgent", 0.3, 0.3);          // score 0.7, 0.7
        noteService.createNote(user.getId(), board.getId(), "DO Urgent & Important", 0.1, 0.1);   // score 0.9, 0.9

        String token = mintValidMcpToken(user.getEmail());

        String callRequest = "{"
                + "\"jsonrpc\":\"2.0\","
                + "\"id\":\"call-list-1\","
                + "\"method\":\"tools/call\","
                + "\"params\":{"
                + "  \"name\":\"list_notes\","
                + "  \"arguments\":{}"
                + "}"
                + "}";

        JsonNode response = performMcpJsonRpc(token, callRequest);

        assertThat(response.get("jsonrpc").asText()).isEqualTo("2.0");
        assertThat(response.get("id").asText()).isEqualTo("call-list-1");
        assertThat(response.has("result")).isTrue();

        JsonNode resultNode = response.get("result");
        if (resultNode.has("isError")) {
            assertThat(resultNode.get("isError").asBoolean()).isFalse();
        }

        JsonNode contentArray = resultNode.get("content");
        assertThat(contentArray.isArray()).isTrue();
        assertThat(contentArray.size()).isGreaterThanOrEqualTo(1);

        String textPayload = contentArray.get(0).get("text").asText();
        List<NoteSummary> summaries = objectMapper.readValue(textPayload, new TypeReference<List<NoteSummary>>() {});

        assertThat(summaries).hasSize(5);

        // Check quadrant ordering: DO -> SCHEDULE -> DELEGATE -> ELIMINATE
        assertThat(summaries.get(0).quadrant()).isEqualTo(Quadrant.DO);
        assertThat(summaries.get(0).text()).isEqualTo("DO Urgent & Important");
        assertThat(summaries.get(0).importance()).isCloseTo(0.9, DOUBLE_TOLERANCE);
        assertThat(summaries.get(0).urgency()).isCloseTo(0.9, DOUBLE_TOLERANCE);
        assertThat(summaries.get(0).board()).isEqualTo("Work Matrix");

        assertThat(summaries.get(1).quadrant()).isEqualTo(Quadrant.DO);
        assertThat(summaries.get(1).text()).isEqualTo("DO Less Urgent");
        assertThat(summaries.get(1).importance()).isCloseTo(0.7, DOUBLE_TOLERANCE);
        assertThat(summaries.get(1).urgency()).isCloseTo(0.7, DOUBLE_TOLERANCE);

        assertThat(summaries.get(2).quadrant()).isEqualTo(Quadrant.SCHEDULE);
        assertThat(summaries.get(2).text()).isEqualTo("SCHEDULE Project Plan");
        assertThat(summaries.get(2).importance()).isCloseTo(0.9, DOUBLE_TOLERANCE);
        assertThat(summaries.get(2).urgency()).isCloseTo(0.2, DOUBLE_TOLERANCE);

        assertThat(summaries.get(3).quadrant()).isEqualTo(Quadrant.DELEGATE);
        assertThat(summaries.get(3).text()).isEqualTo("DELEGATE Book flight");
        assertThat(summaries.get(3).importance()).isCloseTo(0.2, DOUBLE_TOLERANCE);
        assertThat(summaries.get(3).urgency()).isCloseTo(0.9, DOUBLE_TOLERANCE);

        assertThat(summaries.get(4).quadrant()).isEqualTo(Quadrant.ELIMINATE);
        assertThat(summaries.get(4).text()).isEqualTo("ELIMINATE Sort junk mail");
        assertThat(summaries.get(4).importance()).isCloseTo(0.2, DOUBLE_TOLERANCE);
        assertThat(summaries.get(4).urgency()).isCloseTo(0.2, DOUBLE_TOLERANCE);
    }

    @Test
    public void testToolsCallListNotesUserIsolationAndMultiBoard() throws Exception {
        EisenhowerUserEntity user1 = userTestingUtils.getValidUser();
        EisenhowerUserEntity user2 = userTestingUtils.getValidUser();

        var board1User1 = boardService.createBoard(user1, "Personal Board");
        var board2User1 = boardService.createBoard(user1, "Side Project Board");
        var boardUser2 = boardService.createBoard(user2, "User 2 Secret Board");

        noteService.createNote(user1.getId(), board1User1.getId(), "User1 Note on Board1", 0.2, 0.2);
        noteService.createNote(user1.getId(), board2User1.getId(), "User1 Note on Board2", 0.3, 0.3);
        noteService.createNote(user2.getId(), boardUser2.getId(), "User2 Private Note", 0.1, 0.1);

        String tokenUser1 = mintValidMcpToken(user1.getEmail());

        String callRequest = "{"
                + "\"jsonrpc\":\"2.0\","
                + "\"id\":\"call-iso-1\","
                + "\"method\":\"tools/call\","
                + "\"params\":{"
                + "  \"name\":\"list_notes\","
                + "  \"arguments\":{}"
                + "}"
                + "}";

        JsonNode response = performMcpJsonRpc(tokenUser1, callRequest);
        String textPayload = response.get("result").get("content").get(0).get("text").asText();
        List<NoteSummary> summaries = objectMapper.readValue(textPayload, new TypeReference<List<NoteSummary>>() {});

        assertThat(summaries).hasSize(2);
        List<String> texts = summaries.stream().map(NoteSummary::text).toList();
        assertThat(texts).containsExactlyInAnyOrder("User1 Note on Board1", "User1 Note on Board2");
        assertThat(texts).doesNotContain("User2 Private Note");
    }

    // ---------------------------------------------------------------------------------------------
    // MCP tools/call for get_note Integration Tests
    // ---------------------------------------------------------------------------------------------

    @Test
    public void testToolsCallGetNoteWithAttachedDocumentUtf8() throws Exception {
        EisenhowerUserEntity user = userTestingUtils.getValidUser();
        var board = boardService.createBoard(user, "Architecture Notes");

        NoteEntity note = noteService.createNote(user.getId(), board.getId(), "Draft RFC 9728 Spec", 0.1, 0.2); // score 0.9, 0.8 -> DO

        String htmlContent = "<article><h1>OAuth 2.1 & MCP</h1><p>Special characters: € àéîôù — 🚀 &amp; emojis</p></article>";
        AttachedDocumentEntity attachedDoc = AttachedDocumentEntity.builder()
                .note(note)
                ._noteId(note.getId())
                .content(htmlContent.getBytes(StandardCharsets.UTF_8))
                .sharingEnabled(false)
                .build();
        attachedDocumentRepository.save(attachedDoc);

        String token = mintValidMcpToken(user.getEmail());

        String callRequest = "{"
                + "\"jsonrpc\":\"2.0\","
                + "\"id\":\"get-note-1\","
                + "\"method\":\"tools/call\","
                + "\"params\":{"
                + "  \"name\":\"get_note\","
                + "  \"arguments\":{\"id\":\"" + note.getId() + "\"}"
                + "}"
                + "}";

        JsonNode response = performMcpJsonRpc(token, callRequest);
        JsonNode resultNode = response.get("result");
        if (resultNode.has("isError")) {
            assertThat(resultNode.get("isError").asBoolean()).isFalse();
        }

        String textPayload = resultNode.get("content").get(0).get("text").asText();
        NoteDetails details = objectMapper.readValue(textPayload, NoteDetails.class);

        assertThat(details.id()).isEqualTo(note.getId());
        assertThat(details.text()).isEqualTo("Draft RFC 9728 Spec");
        assertThat(details.quadrant()).isEqualTo(Quadrant.DO);
        assertThat(details.importance()).isCloseTo(0.9, DOUBLE_TOLERANCE);
        assertThat(details.urgency()).isCloseTo(0.8, DOUBLE_TOLERANCE);
        assertThat(details.board()).isEqualTo("Architecture Notes");
        assertThat(details.document()).isEqualTo(htmlContent);
    }

    @Test
    public void testToolsCallGetNoteWithoutDocumentReturnsNull() throws Exception {
        EisenhowerUserEntity user = userTestingUtils.getValidUser();
        var board = boardService.createBoard(user, "Simple Board");

        NoteEntity note = noteService.createNote(user.getId(), board.getId(), "Simple Note No Doc", 0.7, 0.2); // score 0.3, 0.8 -> DELEGATE

        String token = mintValidMcpToken(user.getEmail());

        String callRequest = "{"
                + "\"jsonrpc\":\"2.0\","
                + "\"id\":\"get-note-nodoc\","
                + "\"method\":\"tools/call\","
                + "\"params\":{"
                + "  \"name\":\"get_note\","
                + "  \"arguments\":{\"id\":\"" + note.getId() + "\"}"
                + "}"
                + "}";

        JsonNode response = performMcpJsonRpc(token, callRequest);
        String textPayload = response.get("result").get("content").get(0).get("text").asText();
        NoteDetails details = objectMapper.readValue(textPayload, NoteDetails.class);

        assertThat(details.id()).isEqualTo(note.getId());
        assertThat(details.text()).isEqualTo("Simple Note No Doc");
        assertThat(details.quadrant()).isEqualTo(Quadrant.DELEGATE);
        assertThat(details.document()).isNull();
    }

    @Test
    public void testToolsCallGetNoteForbiddenForOtherUsersNoteReturnsMcpError() throws Exception {
        EisenhowerUserEntity user1 = userTestingUtils.getValidUser();
        EisenhowerUserEntity user2 = userTestingUtils.getValidUser();

        var boardUser2 = boardService.createBoard(user2, "User 2 Confidential");
        NoteEntity noteUser2 = noteService.createNote(user2.getId(), boardUser2.getId(), "User 2 Secret Strategy", 0.1, 0.1);

        String tokenUser1 = mintValidMcpToken(user1.getEmail());

        String callRequest = "{"
                + "\"jsonrpc\":\"2.0\","
                + "\"id\":\"get-note-forbidden\","
                + "\"method\":\"tools/call\","
                + "\"params\":{"
                + "  \"name\":\"get_note\","
                + "  \"arguments\":{\"id\":\"" + noteUser2.getId() + "\"}"
                + "}"
                + "}";

        JsonNode response = performMcpJsonRpc(tokenUser1, callRequest);

        // Spring AI MCP returns isError: true on tool exception or JSON-RPC error
        if (response.has("result") && response.get("result").has("isError")) {
            assertThat(response.get("result").get("isError").asBoolean()).isTrue();
        } else if (response.has("error")) {
            assertThat(response.get("error")).isNotNull();
        } else {
            throw new AssertionError("Expected MCP tool error for unauthorized note access, but got: " + response);
        }
    }

    @Test
    public void testToolsCallGetNoteNotFoundAndBadUuidReturnsMcpError() throws Exception {
        EisenhowerUserEntity user = userTestingUtils.getValidUser();
        String token = mintValidMcpToken(user.getEmail());

        // 1. Non-existent UUID
        UUID nonExistentId = UUID.randomUUID();
        String callNonExistent = "{"
                + "\"jsonrpc\":\"2.0\","
                + "\"id\":\"get-note-notfound\","
                + "\"method\":\"tools/call\","
                + "\"params\":{"
                + "  \"name\":\"get_note\","
                + "  \"arguments\":{\"id\":\"" + nonExistentId + "\"}"
                + "}"
                + "}";

        JsonNode respNotFound = performMcpJsonRpc(token, callNonExistent);
        if (respNotFound.has("result") && respNotFound.get("result").has("isError")) {
            assertThat(respNotFound.get("result").get("isError").asBoolean()).isTrue();
        } else if (respNotFound.has("error")) {
            assertThat(respNotFound.get("error")).isNotNull();
        } else {
            throw new AssertionError("Expected MCP error for non-existent note, but got: " + respNotFound);
        }

        // 2. Malformed / bad UUID string
        String callBadUuid = "{"
                + "\"jsonrpc\":\"2.0\","
                + "\"id\":\"get-note-baduuid\","
                + "\"method\":\"tools/call\","
                + "\"params\":{"
                + "  \"name\":\"get_note\","
                + "  \"arguments\":{\"id\":\"not-a-valid-uuid-12345\"}"
                + "}"
                + "}";

        JsonNode respBadUuid = performMcpJsonRpc(token, callBadUuid);
        if (respBadUuid.has("result") && respBadUuid.get("result").has("isError")) {
            assertThat(respBadUuid.get("result").get("isError").asBoolean()).isTrue();
        } else if (respBadUuid.has("error")) {
            assertThat(respBadUuid.get("error")).isNotNull();
        } else {
            throw new AssertionError("Expected MCP error for bad UUID, but got: " + respBadUuid);
        }
    }
}
