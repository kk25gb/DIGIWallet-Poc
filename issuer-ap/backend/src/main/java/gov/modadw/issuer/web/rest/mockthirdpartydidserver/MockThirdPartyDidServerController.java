package gov.modadw.issuer.web.rest.mockthirdpartydidserver;

import com.fasterxml.jackson.databind.ObjectMapper;
import gov.modadw.issuer.entity.ApIssuerDid;
import gov.modadw.issuer.repository.ApIssuerDidRepository;
import gov.modadw.issuer.service.did.IssuerDidService;
import gov.modadw.issuer.service.did.dto.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Mock Third-Party DID Server — simulates the government-operated DID management service.
 *
 * <p>In production this would be an independent service that:
 *
 * <ul>
 *   <li>Generates DID Documents from public keys ({@code url.frontend.generate_did})
 *   <li>Registers (creates) signed DIDs ({@code url.frontend.create_did})
 *   <li>Resolves issuer DID info ({@code url.frontend.get_issuer_info_did})
 * </ul>
 *
 * <p>For POC purposes it is embedded in issuer-ap, reads/writes the local {@code ap_issuer_did}
 * table, and is protected by the same Access-Token mechanism used by other core-system callbacks.
 *
 * <p>Configure vc-handler settings to point here:
 *
 * <pre>
 *   url.frontend.generate_did       → http://{host}:8080/api/mock-did-server/generate
 *   url.frontend.create_did         → http://{host}:8080/api/mock-did-server/create
 *   url.frontend.get_issuer_info_did → http://{host}:8080/api/mock-did-server/
 * </pre>
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/mock-did-server")
public class MockThirdPartyDidServerController {

    private final IssuerDidService issuerDidService;
    private final ApIssuerDidRepository issuerDidRepository;
    private final ObjectMapper objectMapper;

    // ── generate_did ──────────────────────────────────────────────────────────

    /**
     * POST /api/mock-did-server/generate
     *
     * <p>Receives a public key JWK from vc-handler and returns a DID Document (did:key method).
     * Corresponds to: {@code url.frontend.generate_did}
     */
    @PostMapping(
            value = "/generate",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DidGenerateResponseDTO> generateDid(
            @RequestBody DidGenerateRequestDTO request) {
        log.info("[MockDidServer] POST /generate — generate DID from publicKeyJwk");
        DidGenerateResponseDTO response = issuerDidService.handleGenerateDid(request);
        return ResponseEntity.ok(response);
    }

    // ── create_did ────────────────────────────────────────────────────────────

    /**
     * POST /api/mock-did-server/create
     *
     * <p>Receives the signed DID JWT + org info from vc-handler and persists it. Corresponds to:
     * {@code url.frontend.create_did}
     */
    @PostMapping(
            value = "/create",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DidCreateResponseDTO> createDid(@RequestBody DidCreateRequestDTO request) {
        log.info("[MockDidServer] POST /create — store signed DID");
        DidCreateResponseDTO response = issuerDidService.handleCreateDid(request);
        return ResponseEntity.ok(response);
    }

    // ── get_issuer_info_did ───────────────────────────────────────────────────

    /**
     * GET /api/mock-did-server/{didId}
     *
     * <p>Returns issuer DID info in the format expected by vc-handler's {@code
     * callFrontendToGetIssuerInfo()}. Corresponds to: {@code url.frontend.get_issuer_info_did}
     *
     * <p>Expected response:
     *
     * <pre>
     * {
     *   "code": 0,
     *   "msg": "success",
     *   "data": {
     *     "did": "did:key:z2dmz...",
     *     "org": { ... },
     *     "status": 1,
     *     "createdAt": 1710000000000,
     *     "updatedAt": 1710000000000
     *   }
     * }
     * </pre>
     */
    @GetMapping("/{didId:.+}")
    public ResponseEntity<Map<String, Object>> getIssuerInfo(@PathVariable String didId) {
        log.info("[MockDidServer] GET issuer info for DID: {}", didId);

        Optional<ApIssuerDid> optEntity = issuerDidRepository.findByDidId(didId);
        if (optEntity.isEmpty()) {
            log.warn("[MockDidServer] DID not found: {}", didId);
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("code", 9001);
            error.put("msg", "did不存在");
            error.put("data", null);
            return ResponseEntity.badRequest().body(error);
        }

        ApIssuerDid entity = optEntity.get();

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("did", entity.getDidId());
        data.put("org", parseJsonOrEmpty(entity.getOrg()));
        data.put("status", "ACTIVE".equals(entity.getStatus()) ? 1 : 0);
        data.put("createdAt", entity.getCreatedDate().toEpochMilli());
        data.put("updatedAt", entity.getCreatedDate().toEpochMilli());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("code", 0);
        response.put("msg", "success");
        response.put("data", data);

        log.info("[MockDidServer] Returning DID info for: {}", didId);
        return ResponseEntity.ok(response);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJsonOrEmpty(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (Exception e) {
            log.warn("[MockDidServer] Failed to parse org JSON: {}", e.getMessage());
            return Map.of();
        }
    }
}
