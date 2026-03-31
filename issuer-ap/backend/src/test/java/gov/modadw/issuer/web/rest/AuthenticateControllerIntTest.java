package gov.modadw.issuer.web.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import gov.modadw.issuer.web.rest.vm.LoginVM;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class AuthenticateControllerIntTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @Test
  void testAuthenticateSuccess() throws Exception {
    LoginVM loginVM = new LoginVM();
    loginVM.setUsername("admin");
    loginVM.setPassword("admin");

    mockMvc
        .perform(
            post("/api/issuer-ap/authenticate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginVM)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id_token").isNotEmpty());
  }

  @Test
  void testAuthenticateFailWrongPassword() throws Exception {
    LoginVM loginVM = new LoginVM();
    loginVM.setUsername("admin");
    loginVM.setPassword("wrongpassword");

    mockMvc
        .perform(
            post("/api/issuer-ap/authenticate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginVM)))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void testAccountWithoutToken() throws Exception {
    mockMvc.perform(get("/api/issuer-ap/account")).andExpect(status().isUnauthorized());
  }

  @Test
  void testAccountWithValidToken() throws Exception {
    // First authenticate to get a token
    LoginVM loginVM = new LoginVM();
    loginVM.setUsername("admin");
    loginVM.setPassword("admin");

    MvcResult result =
        mockMvc
            .perform(
                post("/api/issuer-ap/authenticate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginVM)))
            .andExpect(status().isOk())
            .andReturn();

    String response = result.getResponse().getContentAsString();
    String token = objectMapper.readTree(response).get("id_token").asText();

    // Use token to access account endpoint
    mockMvc
        .perform(get("/api/issuer-ap/account").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.username").value("admin"))
        .andExpect(jsonPath("$.authorities").isArray());
  }
}
