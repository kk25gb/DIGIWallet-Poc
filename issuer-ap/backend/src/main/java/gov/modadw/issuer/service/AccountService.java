package gov.modadw.issuer.service;

import gov.modadw.issuer.entity.ApUser;
import gov.modadw.issuer.repository.ApUserRepository;
import gov.modadw.issuer.security.SecurityUtils;
import gov.modadw.issuer.web.rest.vm.AccountVM;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AccountService {

  private final ApUserRepository apUserRepository;

  public AccountService(ApUserRepository apUserRepository) {
    this.apUserRepository = apUserRepository;
  }

  public Optional<AccountVM> getCurrentAccount() {
    return SecurityUtils.getCurrentUserLogin()
        .flatMap(apUserRepository::findByUsername)
        .map(this::toAccountVM);
  }

  private AccountVM toAccountVM(ApUser user) {
    return new AccountVM(user.getUsername(), Set.of(user.getAuthority()));
  }
}
