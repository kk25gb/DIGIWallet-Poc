package gov.modadw.issuer.config;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CustomJwtGrantedAuthoritiesConverter
    implements Converter<Jwt, Collection<GrantedAuthority>> {

  @Override
  public Collection<GrantedAuthority> convert(Jwt jwt) {
    String authClaim = jwt.getClaimAsString("auth");
    if (authClaim == null || authClaim.isBlank()) {
      return Collections.emptyList();
    }
    List<GrantedAuthority> authorities =
        Arrays.stream(authClaim.split(","))
            .map(String::trim)
            .map(SimpleGrantedAuthority::new)
            .collect(Collectors.toList());
    return authorities;
  }
}
