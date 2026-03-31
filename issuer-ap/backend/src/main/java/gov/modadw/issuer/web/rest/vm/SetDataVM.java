package gov.modadw.issuer.web.rest.vm;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SetDataVM {

  @NotBlank private String credentialType;

  @NotBlank private String transactionId;

  @NotEmpty private Map<String, Object> data;

  private Map<String, Object> options;
}
