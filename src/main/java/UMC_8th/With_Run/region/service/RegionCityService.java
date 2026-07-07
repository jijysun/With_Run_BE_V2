package UMC_8th.With_Run.region.service;

import UMC_8th.With_Run.region.dto.RegionCityDTO;
import UMC_8th.With_Run.user.repository.RegionCityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RegionCityService {

    private final RegionCityRepository regionCityRepository;

    public List<RegionCityDTO> getCitiesByProvince(Long provinceId) {
        return regionCityRepository.findByProvinceIdOrderByNameAsc(provinceId).stream()
                .map(RegionCityDTO::fromEntity)
                .collect(Collectors.toList());
    }

}
