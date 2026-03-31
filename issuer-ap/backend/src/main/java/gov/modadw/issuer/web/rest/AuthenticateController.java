package gov.modadw.issuer.web.rest;

import static gov.modadw.issuer.security.SecurityUtils.AUTHORITIES_KEY;
import static gov.modadw.issuer.security.SecurityUtils.JWT_ALGORITHM;

import com.fasterxml.jackson.annotation.JsonProperty;
import gov.modadw.issuer.config.ApplicationProperties;
import gov.modadw.issuer.web.rest.vm.LoginVM;
import jakarta.validation.Valid;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/issuer-ap")
public class AuthenticateController {

  private static final Logger log = LoggerFactory.getLogger(AuthenticateController.class);

  private final JwtEncoder jwtEncoder;
  private final AuthenticationManager authenticationManager;
  private final ApplicationProperties applicationProperties;

  public AuthenticateController(
      JwtEncoder jwtEncoder,
      AuthenticationManager authenticationManager,
      ApplicationProperties applicationProperties) {
    this.jwtEncoder = jwtEncoder;
    this.authenticationManager = authenticationManager;
    this.applicationProperties = applicationProperties;
  }

  @PostMapping("/authenticate")
  public ResponseEntity<JWTToken> authorize(@Valid @RequestBody LoginVM loginVM) {
    log.debug("REST request to authenticate user: {}", loginVM.getUsername());

    UsernamePasswordAuthenticationToken authenticationToken =
        new UsernamePasswordAuthenticationToken(loginVM.getUsername(), loginVM.getPassword());

    Authentication authentication = authenticationManager.authenticate(authenticationToken);
    SecurityContextHolder.getContext().setAuthentication(authentication);

    String jwt = createToken(authentication, loginVM.isRememberMe());

    return new ResponseEntity<>(new JWTToken(jwt), HttpStatus.OK);
  }

  private String createToken(Authentication authentication, boolean rememberMe) {
    String authorities =
        authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.joining(","));

    Instant now = Instant.now();
    ApplicationProperties.Security.Jwt jwtProps = applicationProperties.getSecurity().getJwt();
    Instant validity;
    if (rememberMe) {
      validity = now.plus(jwtProps.getTokenValidityInSecondsForRememberMe(), ChronoUnit.SECONDS);
    } else {
      validity = now.plus(jwtProps.getTokenValidityInSeconds(), ChronoUnit.SECONDS);
    }

    JwsHeader jwsHeader = JwsHeader.with(JWT_ALGORITHM).build();
    JwtClaimsSet claims =
        JwtClaimsSet.builder()
            .issuedAt(now)
            .expiresAt(validity)
            .subject(authentication.getName())
            .claim(AUTHORITIES_KEY, authorities)
            .build();

    return this.jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader, claims)).getTokenValue();
  }

  static class JWTToken {

    private String idToken;

    JWTToken(String idToken) {
      this.idToken = idToken;
    }

    @JsonProperty("id_token")
    String getIdToken() {
      return idToken;
    }

    void setIdToken(String idToken) {
      this.idToken = idToken;
    }
  }
}
