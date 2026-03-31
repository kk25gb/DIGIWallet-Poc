package gov.modadw.issuer.service.integration;

import java.util.Map;

/**
 * Client interface for communicating with twdiw-oid4vci-handler. Phase 2+: issuer config,
 * credential offer management, etc.
 */
public interface Oid4vciHandlerClient {

  /**
   * Generate a QR code for credential issuance.
   *
   * @param issuerId the issuer ID (businessId / vc_id)
   * @param body request body containing "authenticated" and "id_token"
   * @return response with qr_code (base64), link, and warnings
   */
  Map<String, Object> generateQrCode(String issuerId, Map<String, Object> body);
}
