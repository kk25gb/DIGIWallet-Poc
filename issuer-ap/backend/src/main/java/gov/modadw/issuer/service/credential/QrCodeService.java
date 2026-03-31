package gov.modadw.issuer.service.credential;

import static gov.modadw.issuer.security.SecurityUtils.JWT_ALGORITHM;

import gov.modadw.issuer.config.IntegrationProperties;
import gov.modadw.issuer.entity.ApCredentialType;
import gov.modadw.issuer.repository.ApCredentialTypeRepository;
import gov.modadw.issuer.service.integration.Oid4vciHandlerClient;
import gov.modadw.issuer.service.integration.VcHandlerClient;
import gov.modadw.issuer.web.rest.vm.QrCodeRequestVM;
import gov.modadw.issuer.web.rest.vm.SetDataVM;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class QrCodeService {

  private final VcHandlerClient vcHandlerClient;
  private final Oid4vciHandlerClient oid4vciHandlerClient;
  private final ApCredentialTypeRepository credentialTypeRepository;
  private final JwtEncoder jwtEncoder;
  private final IntegrationProperties integrationProperties;

  /**
   * Preload holder data to vc-handler via /api/setdata.
   *
   * @return vc-handler response
   */
  public Map<String, Object> preloadData(SetDataVM vm) {
    log.info(
        "Preloading data for credentialType={}, transactionId={}",
        vm.getCredentialType(),
        vm.getTransactionId());

    // Verify credential type exists locally
    credentialTypeRepository
        .findByCredentialType(vm.getCredentialType())
        .orElseThrow(
            () ->
                new IllegalArgumentException(
                    "Credential type not found: " + vm.getCredentialType()));

    // Build setdata request body
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("transactionId", vm.getTransactionId());
    body.put("credentialType", vm.getCredentialType());
    body.put("data", vm.getData());
    if (vm.getOptions() != null && !vm.getOptions().isEmpty()) {
      body.put("options", vm.getOptions());
    }

    Map<String, Object> result = vcHandlerClient.setData(body);
    log.info("Preload data result: {}", result);
    return result;
  }

  /**
   * Generate a QR code for credential issuance via oid4vci-handler.
   *
   * <p>Constructs an id_token JWT with the required claims and calls POST
   * /api/issuer/{issuerId}/qr-code.
   *
   * @return response containing qr_code (base64), link, and warnings
   */
  public Map<String, Object> generateQrCode(QrCodeRequestVM vm) {
    log.info(
        "Generating QR code for credentialType={}, transactionId={}",
        vm.getCredentialType(),
        vm.getTransactionId());

    ApCredentialType entity =
        credentialTypeRepository
            .findByCredentialType(vm.getCredentialType())
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "Credential type not found: " + vm.getCredentialType()));

    // businessId maps to credential_issuer_config.vc_id (the issuerId)
    String issuerId = entity.getBusinessId();

    // Build id_token JWT
    String idToken = buildIdToken(vm, issuerId);

    // Build request body for oid4vci-handler
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("authenticated", true);
    body.put("id_token", idToken);

    Map<String, Object> result = oid4vciHandlerClient.generateQrCode(issuerId, body);
    log.info("Generate QR code result error_code: {}", result.get("error_code"));
    return result;
  }

  /**
   * Build an id_token JWT for oid4vci-handler.
   *
   * <p>Claims: sub (transactionId), aud (issuerId), exp (now+5min), nonce (UUID),
   * credential_configuration_id (credentialType), tx_code (optional).
   */
  private String buildIdToken(QrCodeRequestVM vm, String issuerId) {
    Instant now = Instant.now();

    JwsHeader jwsHeader = JwsHeader.with(JWT_ALGORITHM).build();

    JwtClaimsSet.Builder claimsBuilder =
        JwtClaimsSet.builder()
            .subject(vm.getTransactionId())
            .audience(java.util.List.of(integrationProperties.getOid4vci().getWalletClientId()))
            .expiresAt(now.plus(5, ChronoUnit.MINUTES))
            .issuedAt(now)
            .claim("nonce", vm.getTransactionId())
            .claim("credential_configuration_id", vm.getCredentialType());

    if (vm.getTxCode() != null && !vm.getTxCode().isBlank()) {
      claimsBuilder.claim("tx_code", vm.getTxCode());
    }

    JwtClaimsSet claims = claimsBuilder.build();
    String token = jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader, claims)).getTokenValue();
    log.debug(
        "Built id_token for QR code generation, sub={}, aud={}", vm.getTransactionId(), issuerId);
    return token;
  }
}
