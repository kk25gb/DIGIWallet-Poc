package gov.modadw.issuer.config;

import static gov.modadw.issuer.security.SecurityUtils.JWT_ALGORITHM;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.util.Base64;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

@Slf4j
@RequiredArgsConstructor
@Configuration
public class SecurityJwtConfiguration {

  private final ApplicationProperties applicationProperties;

  @Bean
  public JwtDecoder jwtDecoder() {
    NimbusJwtDecoder jwtDecoder =
        NimbusJwtDecoder.withSecretKey(getSecretKey()).macAlgorithm(JWT_ALGORITHM).build();
    return token -> {
      try {
        return jwtDecoder.decode(token);
      } catch (Exception e) {
        log.error("JWT decode error: {}", e.getMessage());
        throw e;
      }
    };
  }

  @Bean
  public JwtEncoder jwtEncoder() {
    return new NimbusJwtEncoder(new ImmutableSecret<>(getSecretKey()));
  }

  private SecretKey getSecretKey() {
    String jwtKey = applicationProperties.getSecurity().getJwt().getBase64Secret();
    byte[] keyBytes = Base64.from(jwtKey).decode();
    return new SecretKeySpec(keyBytes, 0, keyBytes.length, JWT_ALGORITHM.getName());
  }
}
