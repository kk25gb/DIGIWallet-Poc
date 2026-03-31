package gov.modadw.issuer.service.did;

import com.fasterxml.jackson.databind.ObjectMapper;
import gov.modadw.issuer.entity.ApIssuerDid;
import gov.modadw.issuer.repository.ApIssuerDidRepository;
import gov.modadw.issuer.service.did.dto.*;
import gov.modadw.issuer.service.integration.VcHandlerClient;
import java.io.IOException;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Service
public class IssuerDidService {

  private final VcHandlerClient vcHandlerClient;
  private final ApIssuerDidRepository apIssuerDidRepository;
  private final ObjectMapper objectMapper;

  /** Forward DID registration request from admin UI to core-system. */
  public Map<String, Object> initiateRegistration(IssuerDidRequestDTO request) {
    log.info("Initiating DID registration via core-system");
    return vcHandlerClient.registerDid(request);
  }

  /**
   * Handle generate_did callback from core-system. Receives a public key JWK and returns a DID
   * Document built using the did:key method (P-256).
   */
  public DidGenerateResponseDTO handleGenerateDid(DidGenerateRequestDTO request) {
    log.info("Callback: generate_did received");

    Map<String, Object> publicKeyJwk = request.getPublicKeyJwk();
    if (publicKeyJwk == null || publicKeyJwk.isEmpty()) {
      log.error("publicKeyJwk is null or empty");
      return DidGenerateResponseDTO.error("publicKeyJwk is required");
    }

    try {
      Map<String, Object> didDocument = DidKeyUtils.buildDidDocument(publicKeyJwk);
      log.info("Generated DID Document with id: {}", didDocument.get("id"));
      return DidGenerateResponseDTO.success(didDocument);
    } catch (Exception e) {
      log.error("Failed to generate DID Document: {}", e.getMessage(), e);
      return DidGenerateResponseDTO.error("Failed to generate DID: " + e.getMessage());
    }
  }

  /** Handle create_did callback from core-system. Receives the signed DID JWT and stores it. */
  @Transactional
  public DidCreateResponseDTO handleCreateDid(DidCreateRequestDTO request) {
    log.info("Callback: create_did received");

    if (request.getDid() == null || request.getDid().isBlank()) {
      log.error("DID JWT is null or blank");
      return DidCreateResponseDTO.error("DID JWT is required");
    }

    try {
      // Extract DID identifier from the JWT payload (second part)
      String didId = extractDidIdFromJwt(request.getDid());

      if (apIssuerDidRepository.existsByDidId(didId)) {
        log.warn("DID already exists: {}", didId);
        return DidCreateResponseDTO.error("DID already exists: " + didId);
      }

      ApIssuerDid entity = new ApIssuerDid();
      entity.setDidId(didId);
      entity.setDidJwt(request.getDid());
      entity.setOrgType(request.getOrgType());
      entity.setP7data(request.getP7data());

      if (request.getOrg() != null) {
        entity.setOrg(objectMapper.writeValueAsString(request.getOrg()));
      }

      apIssuerDidRepository.save(entity);
      log.info("DID saved successfully: {}", didId);
      return DidCreateResponseDTO.success();

    } catch (Exception e) {
      log.error("Failed to create DID: {}", e.getMessage(), e);
      return DidCreateResponseDTO.error("Failed to store DID: " + e.getMessage());
    }
  }

  /**
   * Extract DID id from a signed JWT. The JWT payload (Base64url decoded) should contain the DID
   * Document with an "id" field.
   */
  @SuppressWarnings("unchecked")
  private String extractDidIdFromJwt(String jwt) throws IOException {
    String[] parts = jwt.split("\\.");
    if (parts.length < 2) {
      throw new IllegalArgumentException("Invalid JWT format");
    }

    byte[] payloadBytes = java.util.Base64.getUrlDecoder().decode(parts[1]);
    Map<String, Object> payload = objectMapper.readValue(payloadBytes, Map.class);

    Object id = payload.get("id");
    if (id instanceof String didId) {
      return didId;
    }

    throw new IllegalArgumentException("Cannot extract DID id from JWT payload");
  }
}
