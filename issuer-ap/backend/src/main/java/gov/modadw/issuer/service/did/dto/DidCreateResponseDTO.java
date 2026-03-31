package gov.modadw.issuer.service.did.dto;

import lombok.Getter;
import lombok.Setter;

/** Response for core-system's create_did callback. */
@Getter
@Setter
public class DidCreateResponseDTO {

  private int code;
  private String msg;

  public static DidCreateResponseDTO success() {
    DidCreateResponseDTO dto = new DidCreateResponseDTO();
    dto.setCode(0);
    dto.setMsg("success");
    return dto;
  }

  public static DidCreateResponseDTO error(String message) {
    DidCreateResponseDTO dto = new DidCreateResponseDTO();
    dto.setCode(-1);
    dto.setMsg(message);
    return dto;
  }
}
