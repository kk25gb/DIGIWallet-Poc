package gov.modadw.issuer.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "ap_credential_type", schema = "issuer_ap")
public class ApCredentialType implements Serializable {

  private static final long serialVersionUID = 1L;

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "apCredentialTypeSeqGen")
  @SequenceGenerator(
      name = "apCredentialTypeSeqGen",
      sequenceName = "ap_credential_type_id_seq",
      allocationSize = 1)
  private Long id;

  @NotNull
  @Column(name = "credential_type", length = 200, unique = true, nullable = false)
  private String credentialType;

  @NotNull
  @Column(name = "business_id", length = 100, nullable = false)
  private String businessId;

  @NotNull
  @Column(name = "schema_id", length = 500, nullable = false)
  private String schemaId;

  @NotNull
  @Column(name = "vc_schema", columnDefinition = "TEXT", nullable = false)
  private String vcSchema;

  @NotNull
  @Column(name = "effective_time_unit", length = 20, nullable = false)
  private String effectiveTimeUnit;

  @NotNull
  @Column(name = "effective_time_value", nullable = false)
  private Integer effectiveTimeValue;

  @NotNull
  @Column(name = "metadata", columnDefinition = "TEXT", nullable = false)
  private String metadata;

  @NotNull
  @Column(name = "enable_tx_code", nullable = false)
  private Boolean enableTxCode = false;

  @NotNull
  @Column(name = "enable_vc_transfer", nullable = false)
  private Boolean enableVcTransfer = false;

  @NotNull
  @Column(length = 20, nullable = false)
  private String status = "ACTIVE";

  @Column(name = "created_date")
  private Instant createdDate = Instant.now();

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof ApCredentialType)) return false;
    return id != null && id.equals(((ApCredentialType) o).id);
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }
}
