package gov.modadw.issuer.service.did.dto;

import java.util.Map;
import lombok.Getter;
import lombok.Setter;

/** Request from core-system's create_did callback. Contains the signed DID JWT and org info. */
@Getter
@Setter
public class DidCreateRequestDTO {

  private String did; // Signed DID JWT string
  private Map<String, Object> org;
  private Integer orgType;
  private String p7data;
}
