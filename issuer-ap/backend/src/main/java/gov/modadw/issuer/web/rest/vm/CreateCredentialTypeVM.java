package gov.modadw.issuer.web.rest.vm;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateCredentialTypeVM {

  @NotBlank private String businessId;

  @NotBlank private String typeName;

  @NotBlank private String schemaId;

  @NotNull @Positive private Integer effectiveTimeValue;

  @NotBlank private String effectiveTimeUnit;

  @NotEmpty private List<FieldDefinition> fields;

  @Getter
  @Setter
  public static class FieldDefinition {
    @NotBlank private String name;
    @NotBlank private String type;
    private boolean required = true;
  }
}
