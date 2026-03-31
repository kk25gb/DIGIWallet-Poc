package gov.modadw.issuer.web.rest;

import gov.modadw.issuer.security.AuthoritiesConstants;
import gov.modadw.issuer.service.did.IssuerDidService;
import gov.modadw.issuer.service.did.dto.IssuerDidRequestDTO;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** REST controller for DID management. Accessible only by ROLE_ADMIN. */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/issuer-ap")
public class IssuerDidController {

  private final IssuerDidService issuerDidService;

  @PostMapping("/did/register")
  @PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
  public ResponseEntity<Map<String, Object>> registerDid(@RequestBody IssuerDidRequestDTO request) {
    log.info("Admin initiated DID registration");
    try {
      Map<String, Object> result = issuerDidService.initiateRegistration(request);
      return ResponseEntity.status(HttpStatus.CREATED).body(result);
    } catch (Exception e) {
      log.error("DID registration failed: {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("error", e.getMessage()));
    }
  }
}
