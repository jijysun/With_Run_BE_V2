package UMC_8th.With_Run.domain.region.dto;

import UMC_8th.With_Run.domain.map.entity.RegionProvince;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RegionProvinceDTO {
    private Long id;
    private String name;

    public static RegionProvinceDTO fromEntity(RegionProvince province) {
        return new RegionProvinceDTO(province.getId(), province.getName());
    }
}
