package gov.modadw.issuer.service.did.dto;

import java.util.Map;
import lombok.Getter;
import lombok.Setter;

/** Response for core-system's generate_did callback. Returns the generated DID Document. */
@Getter
@Setter
public class DidGenerateResponseDTO {

  private int code;
  private String msg;
  private Map<String, Object> data;

  public static DidGenerateResponseDTO success(Map<String, Object> didDocument) {
    DidGenerateResponseDTO dto = new DidGenerateResponseDTO();
    dto.setCode(0);
    dto.setMsg("success");
    dto.setData(Map.of("did", didDocument));
    return dto;
  }

  public static DidGenerateResponseDTO error(String message) {
    DidGenerateResponseDTO dto = new DidGenerateResponseDTO();
    dto.setCode(-1);
    dto.setMsg(message);
    return dto;
  }
}
