package gov.modadw.issuer.config;

import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@RequiredArgsConstructor
@Configuration
public class CorsConfiguration {

  private final ApplicationProperties applicationProperties;

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    ApplicationProperties.Cors corsProps = applicationProperties.getCors();
    org.springframework.web.cors.CorsConfiguration config =
        new org.springframework.web.cors.CorsConfiguration();

    if (corsProps.getAllowedOrigins() != null && !corsProps.getAllowedOrigins().isBlank()) {
      config.setAllowedOrigins(Arrays.asList(corsProps.getAllowedOrigins().split(",")));
    }
    config.setAllowedMethods(Arrays.asList(corsProps.getAllowedMethods().split(",")));
    config.setAllowedHeaders(Arrays.asList(corsProps.getAllowedHeaders().split(",")));
    config.setAllowCredentials(corsProps.isAllowCredentials());

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/api/**", config);
    return source;
  }
}
