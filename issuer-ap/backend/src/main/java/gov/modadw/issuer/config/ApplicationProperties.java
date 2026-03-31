package gov.modadw.issuer.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "app")
public class ApplicationProperties {

  private Security security = new Security();
  private Cors cors = new Cors();

  @Getter
  @Setter
  public static class Security {

    private Jwt jwt = new Jwt();

    @Getter
    @Setter
    public static class Jwt {
      private String base64Secret = "";
      private long tokenValidityInSeconds = 86400;
      private long tokenValidityInSecondsForRememberMe = 2592000;
    }
  }

  @Getter
  @Setter
  public static class Cors {
    private String allowedOrigins = "";
    private String allowedMethods = "*";
    private String allowedHeaders = "*";
    private boolean allowCredentials = true;
  }
}
