package gov.modadw.issuer.repository;

import gov.modadw.issuer.entity.ApIssuerDid;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ApIssuerDidRepository extends JpaRepository<ApIssuerDid, Long> {

  Optional<ApIssuerDid> findByDidId(String didId);

  boolean existsByDidId(String didId);
}
