package gov.modadw.issuer.security;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;

public final class SecurityUtils {

  public static final MacAlgorithm JWT_ALGORITHM = MacAlgorithm.HS512;

  public static final String AUTHORITIES_KEY = "auth";

  private SecurityUtils() {}

  public static Optional<String> getCurrentUserLogin() {
    SecurityContext securityContext = SecurityContextHolder.getContext();
    return Optional.ofNullable(extractPrincipal(securityContext.getAuthentication()));
  }

  private static String extractPrincipal(Authentication authentication) {
    if (authentication == null) {
      return null;
    } else if (authentication.getPrincipal() instanceof UserDetails springSecurityUser) {
      return springSecurityUser.getUsername();
    } else if (authentication.getPrincipal() instanceof Jwt jwt) {
      return jwt.getSubject();
    } else if (authentication.getPrincipal() instanceof String s) {
      return s;
    }
    return null;
  }

  public static Optional<String> getCurrentUserJWT() {
    SecurityContext securityContext = SecurityContextHolder.getContext();
    return Optional.ofNullable(securityContext.getAuthentication())
        .filter(authentication -> authentication.getCredentials() instanceof String)
        .map(authentication -> (String) authentication.getCredentials());
  }

  public static boolean isAuthenticated() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    return authentication != null
        && getAuthorities(authentication).noneMatch(AuthoritiesConstants.ANONYMOUS::equals);
  }

  public static boolean hasCurrentUserAnyOfAuthorities(String... authorities) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    return (authentication != null
        && getAuthorities(authentication)
            .anyMatch(authority -> Arrays.asList(authorities).contains(authority)));
  }

  public static boolean hasCurrentUserNoneOfAuthorities(String... authorities) {
    return !hasCurrentUserAnyOfAuthorities(authorities);
  }

  public static boolean hasCurrentUserThisAuthority(String authority) {
    return hasCurrentUserAnyOfAuthorities(authority);
  }

  private static Stream<String> getAuthorities(Authentication authentication) {
    return authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority);
  }
}
