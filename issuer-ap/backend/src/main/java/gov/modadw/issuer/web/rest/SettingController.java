package gov.modadw.issuer.web.rest;

import gov.modadw.issuer.security.AuthoritiesConstants;
import gov.modadw.issuer.service.setting.SettingService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** REST controller for vc-handler system settings management. */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/issuer-ap/settings")
public class SettingController {

  private final SettingService settingService;

  @GetMapping
  @PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
  public ResponseEntity<?> checkSetting() {
    log.info("Admin checking system settings");
    try {
      Map<String, Object> result = settingService.checkSetting();
      return ResponseEntity.ok(result);
    } catch (Exception e) {
      log.error("Check setting failed: {}", e.getMessage());
      return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
    }
  }

  @PutMapping
  @PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
  public ResponseEntity<?> updateSetting(@RequestBody Map<String, String> updates) {
    log.info("Admin updating system settings");
    try {
      Map<String, Object> result = settingService.updateSetting(updates);
      return ResponseEntity.ok(result);
    } catch (Exception e) {
      log.error("Update setting failed: {}", e.getMessage());
      return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
    }
  }
}
