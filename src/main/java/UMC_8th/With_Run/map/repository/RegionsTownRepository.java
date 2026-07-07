package UMC_8th.With_Run.map.repository;

import UMC_8th.With_Run.map.entity.RegionsCity;
import UMC_8th.With_Run.map.entity.RegionsTown;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RegionsTownRepository extends JpaRepository<RegionsTown, Long> {
}