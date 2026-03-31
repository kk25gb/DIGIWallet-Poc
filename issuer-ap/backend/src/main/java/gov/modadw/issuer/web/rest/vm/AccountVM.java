package gov.modadw.issuer.web.rest.vm;

import java.util.Set;

public class AccountVM {

  private String username;
  private Set<String> authorities;

  public AccountVM() {}

  public AccountVM(String username, Set<String> authorities) {
    this.username = username;
    this.authorities = authorities;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public Set<String> getAuthorities() {
    return authorities;
  }

  public void setAuthorities(Set<String> authorities) {
    this.authorities = authorities;
  }
}
