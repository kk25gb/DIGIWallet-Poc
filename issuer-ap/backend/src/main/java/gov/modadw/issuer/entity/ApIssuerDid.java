package gov.modadw.issuer.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@Entity
@Table(name = "ap_issuer_did", schema = "issuer_ap")
public class ApIssuerDid implements Serializable {

  private static final long serialVersionUID = 1L;

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "apIssuerDidSeqGen")
  @SequenceGenerator(
      name = "apIssuerDidSeqGen",
      sequenceName = "ap_issuer_did_id_seq",
      allocationSize = 1)
  private Long id;

  @NotNull
  @Column(name = "did_id", length = 500, unique = true, nullable = false)
  private String didId;

  @NotNull
  @Column(name = "did_jwt", columnDefinition = "TEXT", nullable = false)
  private String didJwt;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "did_document", columnDefinition = "jsonb")
  private String didDocument;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "org", columnDefinition = "jsonb")
  private String org;

  @Column(name = "org_type")
  private Integer orgType;

  @Column(name = "p7data", columnDefinition = "TEXT")
  private String p7data;

  @NotNull
  @Column(length = 20, nullable = false)
  private String status = "ACTIVE";

  @Column(name = "created_date")
  private Instant createdDate = Instant.now();

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof ApIssuerDid)) return false;
    return id != null && id.equals(((ApIssuerDid) o).id);
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }
}
