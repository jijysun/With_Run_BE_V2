package UMC_8th.With_Run.region.dto;

import UMC_8th.With_Run.map.entity.RegionProvince;
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
