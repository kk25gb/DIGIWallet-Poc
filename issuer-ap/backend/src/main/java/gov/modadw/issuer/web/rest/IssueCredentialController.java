package gov.modadw.issuer.web.rest;

import gov.modadw.issuer.security.AuthoritiesConstants;
import gov.modadw.issuer.service.credential.QrCodeService;
import gov.modadw.issuer.web.rest.vm.QrCodeRequestVM;
import gov.modadw.issuer.web.rest.vm.SetDataVM;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/issuer-ap/issue")
public class IssueCredentialController {

  private final QrCodeService qrCodeService;

  @PostMapping("/preload")
  @PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
  public ResponseEntity<?> preloadData(@Valid @RequestBody SetDataVM vm) {
    log.info(
        "Admin preloading data for credentialType={}, transactionId={}",
        vm.getCredentialType(),
        vm.getTransactionId());
    try {
      Map<String, Object> result = qrCodeService.preloadData(vm);
      return ResponseEntity.status(HttpStatus.CREATED).body(result);
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    } catch (Exception e) {
      log.error("Preload data failed: {}", e.getMessage());
      return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
    }
  }

  @PostMapping("/qr-code")
  @PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
  public ResponseEntity<?> generateQrCode(@Valid @RequestBody QrCodeRequestVM vm) {
    log.info(
        "Admin generating QR code for credentialType={}, transactionId={}",
        vm.getCredentialType(),
        vm.getTransactionId());
    try {
      Map<String, Object> result = qrCodeService.generateQrCode(vm);
      return ResponseEntity.ok(result);
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    } catch (Exception e) {
      log.error("Generate QR code failed: {}", e.getMessage());
      return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
    }
  }
}
