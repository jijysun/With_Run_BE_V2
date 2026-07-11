package UMC_8th.With_Run.domain.region.dto;

import UMC_8th.With_Run.domain.map.entity.RegionsTown;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RegionTownDTO {
    private Long id;
    private String name;

    public static RegionTownDTO fromEntity(RegionsTown town) {
        return new RegionTownDTO(town.getId(), town.getName());
    }
}

