package gov.modadw.issuer.web.rest.vm;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class QrCodeRequestVM {

  @NotBlank private String credentialType;

  @NotBlank private String transactionId;

  private String txCode;
}
