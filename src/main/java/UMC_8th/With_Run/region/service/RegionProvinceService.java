package UMC_8th.With_Run.region.service;

import UMC_8th.With_Run.map.entity.RegionProvince;
import UMC_8th.With_Run.region.dto.RegionProvinceDTO;
import UMC_8th.With_Run.user.repository.RegionProvinceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RegionProvinceService {

    private final RegionProvinceRepository regionProvinceRepository;
    public List<RegionProvinceDTO> getAllProvinces() {
        List<String> customOrder = List.of(
                "서울", "경기", "인천", "부산", "대구", "경남", "경북",
                "충남", "충북", "전남", "전북", "광주", "대전",
                "울산", "강원", "세종", "제주"
        );

        List<RegionProvince> provinces = regionProvinceRepository.findAll();

        // 커스텀 순서대로 정렬
        provinces.sort(Comparator.comparingInt(p -> customOrder.indexOf(p.getName())));

        return provinces.stream()
                .map(RegionProvinceDTO::fromEntity)
                .collect(Collectors.toList());
    }

}
