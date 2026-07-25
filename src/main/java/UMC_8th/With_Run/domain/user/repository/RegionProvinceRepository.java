package UMC_8th.With_Run.domain.user.repository;

import UMC_8th.With_Run.domain.map.entity.RegionProvince;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RegionProvinceRepository extends JpaRepository<RegionProvince, Long> {

}