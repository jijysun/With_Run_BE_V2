package UMC_8th.With_Run.user.repository;

import UMC_8th.With_Run.map.entity.RegionsCity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Arrays;
import java.util.List;

@Repository
public interface RegionCityRepository extends JpaRepository<RegionsCity, Long> {

    List<RegionsCity> findByProvinceIdOrderByNameAsc(Long provinceId);
}
