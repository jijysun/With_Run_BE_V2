package UMC_8th.With_Run.domain.region.dto;

import UMC_8th.With_Run.domain.map.entity.RegionsCity;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RegionCityDTO {
    private Long id;
    private String name;

    public static RegionCityDTO fromEntity(RegionsCity city) {
        return new RegionCityDTO(city.getId(), city.getName());
    }
}