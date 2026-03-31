package gov.modadw.issuer.service.integration;

import gov.modadw.issuer.service.did.dto.IssuerDidRequestDTO;
import java.util.Map;

/** Client interface for communicating with twdiw-vc-handler. */
public interface VcHandlerClient {

  /**
   * Forward DID registration request to core-system POST /api/did.
   *
   * @param request the DID registration request containing org info and p7data
   * @return response from core-system
   */
  Map<String, Object> registerDid(IssuerDidRequestDTO request);

  /** GET /api/checksetting — check current system settings. */
  Map<String, Object> checkSetting();

  /** POST /api/updatesetting — update system settings. */
  Map<String, Object> updateSetting(Map<String, String> updates);

  /** POST /api/setseq/{credentialType} — create credential type sequence and policy. */
  Map<String, Object> createSequence(String credentialType, Map<String, Object> body);

  /** POST /api/delseq/{credentialType} — delete credential type sequence and policy. */
  Map<String, Object> deleteSequence(String credentialType, Map<String, Object> body);

  /** POST /api/funcswitch/{credentialType} — set function switches for credential type. */
  Map<String, Object> setFuncSwitch(String credentialType, Map<String, Object> switches);

  /** POST /api/setdata — preload holder credential data. */
  Map<String, Object> setData(Map<String, Object> body);
}
