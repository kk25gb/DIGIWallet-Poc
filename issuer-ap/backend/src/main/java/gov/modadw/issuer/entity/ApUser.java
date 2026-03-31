package gov.modadw.issuer.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import java.time.Instant;
import java.util.Locale;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "ap_user", schema = "issuer_ap")
public class ApUser implements Serializable {

  private static final long serialVersionUID = 1L;

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "apUserSeqGen")
  @SequenceGenerator(name = "apUserSeqGen", sequenceName = "ap_user_id_seq", allocationSize = 1)
  private Long id;

  @NotNull
  @Size(min = 1, max = 50)
  @Column(length = 50, unique = true, nullable = false)
  private String username;

  @JsonIgnore
  @NotNull
  @Size(max = 60)
  @Column(name = "password_hash", length = 60, nullable = false)
  private String passwordHash;

  @NotNull
  @Size(max = 20)
  @Column(length = 20, nullable = false)
  private String status = "ACTIVE";

  @Column(name = "created_date")
  private Instant createdDate = Instant.now();

  @Column(name = "last_modified_date")
  private Instant lastModifiedDate = Instant.now();

  @NotNull
  @Size(max = 50)
  @Column(length = 50, nullable = false)
  private String authority = "ROLE_OPERATOR";

  public void setUsername(String username) {
    this.username = (username != null) ? username.toLowerCase(Locale.ENGLISH) : null;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof ApUser)) return false;
    return id != null && id.equals(((ApUser) o).id);
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }

  @Override
  public String toString() {
    return "ApUser{" + "username='" + username + '\'' + ", status='" + status + '\'' + "}";
  }
}
