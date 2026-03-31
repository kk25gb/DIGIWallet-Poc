package gov.modadw.issuer.web.rest;

import gov.modadw.issuer.service.AccountService;
import gov.modadw.issuer.web.rest.vm.AccountVM;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/issuer-ap")
public class AccountController {

  private final AccountService accountService;

  public AccountController(AccountService accountService) {
    this.accountService = accountService;
  }

  @GetMapping("/account")
  public ResponseEntity<AccountVM> getAccount() {
    return accountService
        .getCurrentAccount()
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }
}
