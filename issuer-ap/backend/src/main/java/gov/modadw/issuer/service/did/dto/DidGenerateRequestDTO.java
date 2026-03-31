package gov.modadw.issuer.service.did.dto;

import java.util.Map;
import lombok.Getter;
import lombok.Setter;

/** Request from core-system's generate_did callback. Contains the public key in JWK format. */
@Getter
@Setter
public class DidGenerateRequestDTO {

  private Map<String, Object> publicKeyJwk;
}
