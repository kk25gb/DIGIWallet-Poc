package gov.modadw.issuer.repository;

import gov.modadw.issuer.entity.ApCredentialType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ApCredentialTypeRepository extends JpaRepository<ApCredentialType, Long> {

  Optional<ApCredentialType> findByCredentialType(String credentialType);

  List<ApCredentialType> findByStatusOrderByCreatedDateDesc(String status);

  boolean existsByCredentialType(String credentialType);
}
