package gov.modadw.issuer.service.integration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.modadw.issuer.config.IntegrationProperties;
import gov.modadw.issuer.service.did.dto.IssuerDidRequestDTO;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@RequiredArgsConstructor
@Component
public class VcHandlerClientImpl implements VcHandlerClient {

  private final RestTemplate restTemplate;
  private final IntegrationProperties integrationProperties;
  private final ObjectMapper objectMapper;

  private String vcBaseUrl() {
    return integrationProperties.getVcHandler().getBaseUrl();
  }

  private HttpHeaders jsonHeaders() {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    return headers;
  }

  @Override
  public Map<String, Object> registerDid(IssuerDidRequestDTO request) {
    String url = vcBaseUrl() + "/api/did";
    log.info("Calling core-system DID registration: {}", url);

    HttpEntity<IssuerDidRequestDTO> entity = new HttpEntity<>(request, jsonHeaders());

    try {
      @SuppressWarnings("unchecked")
      ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
      log.info("Core-system DID registration response status: {}", response.getStatusCode());
      return response.getBody();
    } catch (RestClientException e) {
      log.error("Failed to call core-system DID registration: {}", e.getMessage());
      throw new RuntimeException("Core-system DID registration failed: " + e.getMessage(), e);
    }
  }

  @Override
  public Map<String, Object> checkSetting() {
    String url = vcBaseUrl() + "/api/checksetting";
    log.info("Calling core-system check setting: {}", url);

    try {
      @SuppressWarnings("unchecked")
      ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, null, Map.class);
      log.info("Core-system check setting response status: {}", response.getStatusCode());
      return response.getBody();
    } catch (RestClientException e) {
      log.error("Failed to call core-system check setting: {}", e.getMessage());
      throw new RuntimeException("Core-system check setting failed: " + e.getMessage(), e);
    }
  }

  @Override
  public Map<String, Object> updateSetting(Map<String, String> updates) {
    String url = vcBaseUrl() + "/api/updatesetting";
    log.info("Calling core-system update setting: {}", url);

    Map<String, Object> body = Map.of("updates", updates);
    HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, jsonHeaders());

    try {
      @SuppressWarnings("unchecked")
      ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
      log.info("Core-system update setting response status: {}", response.getStatusCode());
      return response.getBody();
    } catch (RestClientException e) {
      log.error("Failed to call core-system update setting: {}", e.getMessage());
      throw new RuntimeException("Core-system update setting failed: " + e.getMessage(), e);
    }
  }

  @Override
  public Map<String, Object> createSequence(String credentialType, Map<String, Object> body) {
    String url = vcBaseUrl() + "/api/setseq/" + credentialType;
    log.info("Calling core-system create sequence: {}", url);

    // vc-handler expects @RequestBody String (raw JSON), not a deserialized object
    String jsonBody;
    try {
      jsonBody = objectMapper.writeValueAsString(body);
    } catch (JsonProcessingException e) {
      throw new RuntimeException("Failed to serialize request body: " + e.getMessage(), e);
    }
    log.info("Create sequence request body: {}", jsonBody);

    HttpEntity<String> entity = new HttpEntity<>(jsonBody, jsonHeaders());

    try {
      @SuppressWarnings("unchecked")
      ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
      log.info("Core-system create sequence response status: {}", response.getStatusCode());
      return response.getBody();
    } catch (RestClientException e) {
      log.error("Failed to call core-system create sequence: {}", e.getMessage());
      throw new RuntimeException("Core-system create sequence failed: " + e.getMessage(), e);
    }
  }

  @Override
  public Map<String, Object> deleteSequence(String credentialType, Map<String, Object> body) {
    String url = vcBaseUrl() + "/api/delseq/" + credentialType;
    log.info("Calling core-system delete sequence: {}", url);

    HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, jsonHeaders());

    try {
      @SuppressWarnings("unchecked")
      ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
      log.info("Core-system delete sequence response status: {}", response.getStatusCode());
      return response.getBody();
    } catch (RestClientException e) {
      log.error("Failed to call core-system delete sequence: {}", e.getMessage());
      throw new RuntimeException("Core-system delete sequence failed: " + e.getMessage(), e);
    }
  }

  @Override
  public Map<String, Object> setFuncSwitch(String credentialType, Map<String, Object> switches) {
    String url = vcBaseUrl() + "/api/funcswitch/" + credentialType;
    log.info("Calling core-system set func switch: {}", url);

    Map<String, Object> body = Map.of("switches", switches);
    HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, jsonHeaders());

    try {
      @SuppressWarnings("unchecked")
      ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
      log.info("Core-system set func switch response status: {}", response.getStatusCode());
      return response.getBody();
    } catch (RestClientException e) {
      log.error("Failed to call core-system set func switch: {}", e.getMessage());
      throw new RuntimeException("Core-system set func switch failed: " + e.getMessage(), e);
    }
  }

  @Override
  public Map<String, Object> setData(Map<String, Object> body) {
    String url = vcBaseUrl() + "/api/setdata";
    log.info("Calling core-system set data: {}", url);

    HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, jsonHeaders());

    try {
      @SuppressWarnings("unchecked")
      ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
      log.info("Core-system set data response status: {}", response.getStatusCode());
      return response.getBody();
    } catch (RestClientException e) {
      log.error("Failed to call core-system set data: {}", e.getMessage());
      throw new RuntimeException("Core-system set data failed: " + e.getMessage(), e);
    }
  }
}
