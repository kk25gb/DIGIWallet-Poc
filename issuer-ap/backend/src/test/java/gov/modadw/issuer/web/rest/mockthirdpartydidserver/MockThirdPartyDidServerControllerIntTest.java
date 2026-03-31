package gov.modadw.issuer.web.rest.mockthirdpartydidserver;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class MockThirdPartyDidServerControllerIntTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @Value("${integration.callback.access-token}")
  private String accessToken;

  @Test
  void testGenerateDidWithoutAccessToken() throws Exception {
    String body =
        objectMapper.writeValueAsString(
            Map.of(
                "publicKeyJwk",
                Map.of(
                    "kty", "EC",
                    "crv", "P-256",
                    "x", "f83OJ3D2xF1Bg8vub9tLe1gHMzV76e8Tus9uPHvRVEU",
                    "y", "x_FEzRu9m36HLN_tue659LNpXW6pCyStikYjKIWI5a0")));

    mockMvc
        .perform(
            post("/api/mock-did-server/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void testGenerateDidWithValidAccessToken() throws Exception {
    String body =
        objectMapper.writeValueAsString(
            Map.of(
                "publicKeyJwk",
                Map.of(
                    "kty", "EC",
                    "crv", "P-256",
                    "x", "f83OJ3D2xF1Bg8vub9tLe1gHMzV76e8Tus9uPHvRVEU",
                    "y", "x_FEzRu9m36HLN_tue659LNpXW6pCyStikYjKIWI5a0")));

    mockMvc
        .perform(
            post("/api/mock-did-server/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Access-Token", accessToken)
                .content(body))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data.did.id").exists());
  }

  @Test
  void testGenerateDidWithInvalidAccessToken() throws Exception {
    String body =
        objectMapper.writeValueAsString(
            Map.of(
                "publicKeyJwk",
                Map.of(
                    "kty", "EC",
                    "crv", "P-256",
                    "x", "f83OJ3D2xF1Bg8vub9tLe1gHMzV76e8Tus9uPHvRVEU",
                    "y", "x_FEzRu9m36HLN_tue659LNpXW6pCyStikYjKIWI5a0")));

    mockMvc
        .perform(
            post("/api/mock-did-server/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Access-Token", "wrong-token")
                .content(body))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void testCreateDidWithValidAccessToken() throws Exception {
    String body =
        objectMapper.writeValueAsString(
            Map.of(
                "did",
                "eyJhbGciOiJFUzI1NiJ9.eyJpZCI6ImRpZDprZXk6dGVzdDEyMyJ9.fake-sig",
                "org",
                Map.of("name", "Test Org"),
                "orgType",
                1,
                "p7data",
                ""));

    mockMvc
        .perform(
            post("/api/mock-did-server/create")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Access-Token", accessToken)
                .content(body))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0));
  }
}
