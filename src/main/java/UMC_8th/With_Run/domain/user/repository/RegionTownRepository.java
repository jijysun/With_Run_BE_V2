package UMC_8th.With_Run.domain.user.repository;

import UMC_8th.With_Run.domain.map.entity.RegionsTown;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RegionTownRepository extends JpaRepository<RegionsTown, Long> {
    List<RegionsTown> findByCityIdOrderByNameAsc(Long cityId);

}