package UMC_8th.With_Run.region.service;

import UMC_8th.With_Run.region.dto.RegionTownDTO;
import UMC_8th.With_Run.user.repository.RegionTownRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RegionTownService {

    private final RegionTownRepository regionTownRepository;

    public List<RegionTownDTO> getTownsByCity(Long cityId) {
        return regionTownRepository.findByCityIdOrderByNameAsc(cityId).stream()
                .map(RegionTownDTO::fromEntity)
                .collect(Collectors.toList());
    }


}
