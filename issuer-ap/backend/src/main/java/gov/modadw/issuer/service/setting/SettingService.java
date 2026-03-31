package gov.modadw.issuer.service.setting;

import gov.modadw.issuer.service.integration.VcHandlerClient;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class SettingService {

  private final VcHandlerClient vcHandlerClient;

  /** Check current vc-handler system settings. */
  public Map<String, Object> checkSetting() {
    log.info("Checking vc-handler system settings");
    return vcHandlerClient.checkSetting();
  }

  /** Update vc-handler system settings. */
  public Map<String, Object> updateSetting(Map<String, String> updates) {
    log.info("Updating vc-handler system settings: {}", updates.keySet());
    return vcHandlerClient.updateSetting(updates);
  }
}
