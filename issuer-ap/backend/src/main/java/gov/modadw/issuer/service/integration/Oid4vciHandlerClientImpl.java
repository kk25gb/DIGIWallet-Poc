package gov.modadw.issuer.service.integration;

import gov.modadw.issuer.config.IntegrationProperties;
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
public class Oid4vciHandlerClientImpl implements Oid4vciHandlerClient {

  private final RestTemplate restTemplate;
  private final IntegrationProperties integrationProperties;

  private String oid4vciBaseUrl() {
    return integrationProperties.getOid4vciHandler().getBaseUrl();
  }

  private HttpHeaders jsonHeaders() {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    return headers;
  }

  @Override
  public Map<String, Object> generateQrCode(String issuerId, Map<String, Object> body) {
    String url = oid4vciBaseUrl() + "/api/issuer/" + issuerId + "/qr-code";
    log.info("Calling oid4vci-handler generate QR code: {}", url);

    HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, jsonHeaders());

    try {
      @SuppressWarnings("unchecked")
      ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
      log.info("OID4VCI generate QR code response status: {}", response.getStatusCode());
      return response.getBody();
    } catch (RestClientException e) {
      log.error("Failed to call oid4vci-handler generate QR code: {}", e.getMessage());
      throw new RuntimeException("OID4VCI generate QR code failed: " + e.getMessage(), e);
    }
  }
}
