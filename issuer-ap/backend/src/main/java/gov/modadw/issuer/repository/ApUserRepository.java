package gov.modadw.issuer.repository;

import gov.modadw.issuer.entity.ApUser;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ApUserRepository extends JpaRepository<ApUser, Long> {

  Optional<ApUser> findByUsername(String username);
}
