package UMC_8th.With_Run.map.repository;

import UMC_8th.With_Run.map.entity.PetFacility;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PetFacilityRepository extends JpaRepository<PetFacility, Long> {
    List<PetFacility> findByCategoryContainingIgnoreCase(String category);

    Page<PetFacility> findByCategoryContainingIgnoreCase(String category, Pageable pageable);

    // 주소 전체를 검색하는 기존 메서드 대신 각 컬럼을 사용하는 메서드 추
    Page<PetFacility> findByProvinceContaining(String province, Pageable pageable);
    Page<PetFacility> findByProvinceContainingAndCityContaining(String province, String city, Pageable pageable);
    Page<PetFacility> findByProvinceContainingAndCityContainingAndTownContaining(String province, String city, String town, Pageable pageable);

    // 카테고리 검색과 지역 검색을 결합한 메서드
    Page<PetFacility> findByCategoryContainingIgnoreCaseAndProvinceContaining(String category, String province, Pageable pageable);
    Page<PetFacility> findByCategoryContainingIgnoreCaseAndProvinceContainingAndCityContaining(String category, String province, String city, Pageable pageable);
    Page<PetFacility> findByCategoryContainingIgnoreCaseAndProvinceContainingAndCityContainingAndTownContaining(String category, String province, String city, String town, Pageable pageable);

    // 기존 메서드 삭제 (Address 필드를 기준으로 한 검색)
    // Page<PetFacility> findByAddressContaining(String address, Pageable pageable);
    // Page<PetFacility> findByCategoryContainingIgnoreCaseAndAddressContaining(String category, String address, Pageable pageable);
}