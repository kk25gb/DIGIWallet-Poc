package gov.modadw.issuer.config;

import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@RequiredArgsConstructor
@Configuration
public class RestClientConfiguration {

  private final IntegrationProperties integrationProperties;

  @Bean
  public RestTemplate restTemplate(RestTemplateBuilder builder) {
    return builder
        .setConnectTimeout(Duration.ofMillis(integrationProperties.getConnectTimeout()))
        .setReadTimeout(Duration.ofMillis(integrationProperties.getReadTimeout()))
        .build();
  }
}
