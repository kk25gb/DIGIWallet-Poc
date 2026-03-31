package gov.modadw.issuer.service.did.dto;

import java.util.Map;
import lombok.Getter;
import lombok.Setter;

/**
 * Request DTO for initiating DID registration from the frontend. Matches the structure expected by
 * core-system's POST /api/did.
 */
@Getter
@Setter
public class IssuerDidRequestDTO {

  private Map<String, Object> org;
  private String p7data;
}
