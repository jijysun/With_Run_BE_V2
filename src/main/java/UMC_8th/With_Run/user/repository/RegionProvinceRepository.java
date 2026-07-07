package UMC_8th.With_Run.user.repository;

import UMC_8th.With_Run.map.entity.RegionProvince;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RegionProvinceRepository extends JpaRepository<RegionProvince, Long> {

}