package gov.modadw.issuer.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Configuration
@ConfigurationProperties(prefix = "integration")
public class IntegrationProperties {

  private final ServiceConfig vcHandler = new ServiceConfig();
  private final ServiceConfig oid4vciHandler = new ServiceConfig();
  private final Oid4vciConfig oid4vci = new Oid4vciConfig();

  @Setter private int connectTimeout = 5000;

  @Setter private int readTimeout = 10000;

  @Getter
  @Setter
  public static class ServiceConfig {
    private String baseUrl = "";
  }

  @Getter
  @Setter
  public static class Oid4vciConfig {
    /** The client_id that the wallet app uses when calling the /token endpoint. */
    private String walletClientId = "moda_dw";

    /** The external base URL of oid4vci-handler that the wallet app can reach. */
    private String externalUrl = "";
  }
}
