package UMC_8th.With_Run.region.controller;

import UMC_8th.With_Run.region.dto.RegionCityDTO;
import UMC_8th.With_Run.region.dto.RegionProvinceDTO;
import UMC_8th.With_Run.region.dto.RegionTownDTO;
import UMC_8th.With_Run.region.service.RegionCityService;
import UMC_8th.With_Run.region.service.RegionProvinceService;
import UMC_8th.With_Run.region.service.RegionTownService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "지역조회 API", description = "지역 변경 기능 내 지역 조회 기능 제공")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/region")
public class RegionController {

    private final RegionProvinceService regionProvinceService;
    private final RegionCityService regionCityService;
    private final RegionTownService regionTownService;

    @Operation(summary = "전체 시/도 조회", description = "전국 시/도를 조회합니다.")
    @GetMapping("/province")
    public List<RegionProvinceDTO> getProvinces() {
        return regionProvinceService.getAllProvinces();
    }

    @Operation(summary = "전체 시/군/구 조회", description = "전국 시/군/구를 조회합니다.")
    @GetMapping("/city")
    public List<RegionCityDTO> getCities(@RequestParam Long provinceId) {
        return regionCityService.getCitiesByProvince(provinceId);
    }

    @Operation(summary = "전체 동/읍/면 조회", description = "전국 동/읍/면을 조회합니다.")
    @GetMapping("/town")
    public List<RegionTownDTO> getTowns(@RequestParam Long cityId) {
        return regionTownService.getTownsByCity(cityId);
    }



}
