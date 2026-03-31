package gov.modadw.issuer.web.rest;

import gov.modadw.issuer.entity.ApCredentialType;
import gov.modadw.issuer.security.AuthoritiesConstants;
import gov.modadw.issuer.service.credential.CredentialTypeService;
import gov.modadw.issuer.web.rest.vm.CreateCredentialTypeVM;
import gov.modadw.issuer.web.rest.vm.FuncSwitchVM;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** REST controller for credential type management. */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/issuer-ap/credential-types")
public class CredentialTypeController {

  private final CredentialTypeService credentialTypeService;

  @GetMapping
  public ResponseEntity<?> list() {
    log.info("listing credential types");
    try {
      List<ApCredentialType> result = credentialTypeService.listCredentialTypes();
      return ResponseEntity.ok(result);
    } catch (Exception e) {
      log.error("List credential types failed: {}", e.getMessage());
      return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
    }
  }

  @GetMapping("/{credentialType}")
  public ResponseEntity<?> get(@PathVariable String credentialType) {
    log.info("getting credential type: {}", credentialType);
    try {
      ApCredentialType result = credentialTypeService.getCredentialType(credentialType);
      return ResponseEntity.ok(result);
    } catch (IllegalArgumentException e) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
    } catch (Exception e) {
      log.error("Get credential type failed: {}", e.getMessage());
      return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
    }
  }

  @PostMapping
  @PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
  public ResponseEntity<?> create(@Valid @RequestBody CreateCredentialTypeVM vm) {
    log.info("Admin creating credential type: {}_{}", vm.getBusinessId(), vm.getTypeName());
    try {
      Map<String, Object> result = credentialTypeService.createCredentialType(vm);
      return ResponseEntity.status(HttpStatus.CREATED).body(result);
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    } catch (Exception e) {
      log.error("Create credential type failed: {}", e.getMessage());
      return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
    }
  }

  @DeleteMapping("/{credentialType}")
  @PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
  public ResponseEntity<?> delete(@PathVariable String credentialType) {
    log.info("Admin deleting credential type: {}", credentialType);
    try {
      Map<String, Object> result = credentialTypeService.deleteCredentialType(credentialType);
      return ResponseEntity.ok(result);
    } catch (IllegalArgumentException e) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
    } catch (Exception e) {
      log.error("Delete credential type failed: {}", e.getMessage());
      return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
    }
  }

  @PutMapping("/{credentialType}/func-switch")
  @PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
  public ResponseEntity<?> setFuncSwitch(
      @PathVariable String credentialType, @RequestBody FuncSwitchVM vm) {
    log.info("Admin setting func switch for: {}", credentialType);
    try {
      Map<String, Object> result = credentialTypeService.setFuncSwitch(credentialType, vm);
      return ResponseEntity.ok(result);
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    } catch (Exception e) {
      log.error("Set func switch failed: {}", e.getMessage());
      return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
    }
  }
}
