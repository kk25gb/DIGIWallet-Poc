package gov.modadw.issuer.security;

import gov.modadw.issuer.entity.ApUser;
import gov.modadw.issuer.repository.ApUserRepository;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component("userDetailsService")
public class DomainUserDetailsService implements UserDetailsService {

  private static final Logger log = LoggerFactory.getLogger(DomainUserDetailsService.class);

  private final ApUserRepository apUserRepository;

  public DomainUserDetailsService(ApUserRepository apUserRepository) {
    this.apUserRepository = apUserRepository;
  }

  @Override
  @Transactional(readOnly = true)
  public UserDetails loadUserByUsername(final String login) {
    log.debug("Authenticating {}", login);

    String lowercaseLogin = login.toLowerCase(Locale.ENGLISH);
    return apUserRepository
        .findByUsername(lowercaseLogin)
        .map(this::createSpringSecurityUser)
        .orElseThrow(
            () ->
                new UsernameNotFoundException(
                    "User " + lowercaseLogin + " was not found in the database"));
  }

  private User createSpringSecurityUser(ApUser apUser) {
    if (!"ACTIVE".equals(apUser.getStatus())) {
      throw new RuntimeException("User " + apUser.getUsername() + " is not active");
    }
    List<SimpleGrantedAuthority> grantedAuthorities =
        List.of(new SimpleGrantedAuthority(apUser.getAuthority()));
    return new User(apUser.getUsername(), apUser.getPasswordHash(), grantedAuthorities);
  }
}
