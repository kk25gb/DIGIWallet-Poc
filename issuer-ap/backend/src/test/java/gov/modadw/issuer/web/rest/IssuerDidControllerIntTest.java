package gov.modadw.issuer.web.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import gov.modadw.issuer.web.rest.vm.LoginVM;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class IssuerDidControllerIntTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @Test
  void testRegisterDidWithoutAuth() throws Exception {
    String body =
        objectMapper.writeValueAsString(Map.of("org", Map.of("name", "Test Org"), "p7data", ""));

    mockMvc
        .perform(
            post("/api/issuer-ap/did/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void testRegisterDidWithAdminTokenReturns5xxWithoutCoreSystem() throws Exception {
    // Get admin JWT
    LoginVM loginVM = new LoginVM();
    loginVM.setUsername("admin");
    loginVM.setPassword("admin");

    MvcResult loginResult =
        mockMvc
            .perform(
                post("/api/issuer-ap/authenticate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginVM)))
            .andExpect(status().isOk())
            .andReturn();

    String token =
        objectMapper
            .readTree(loginResult.getResponse().getContentAsString())
            .get("id_token")
            .asText();

    // Call did/register - should fail since core-system is not running
    String body =
        objectMapper.writeValueAsString(Map.of("org", Map.of("name", "Test Org"), "p7data", ""));

    mockMvc
        .perform(
            post("/api/issuer-ap/did/register")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token)
                .content(body))
        .andExpect(status().is5xxServerError());
  }
}
