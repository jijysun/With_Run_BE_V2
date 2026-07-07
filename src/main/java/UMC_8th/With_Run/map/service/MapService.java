package UMC_8th.With_Run.map.service;

import UMC_8th.With_Run.map.dto.MapRequestDTO;
import UMC_8th.With_Run.map.dto.MapResponseDTO;

public interface MapService {

    // 수정: 카테고리 검색 시 위치 정보를 추가로 받아 필터링하도록 변경
    MapResponseDTO.PetFacilityPageResponseDto getPetFacilityByCategory(String category, String region_province, String regions_city, String regions_town, int page, int size);

    // 수정: ID 검색 시 위치 정보를 추가로 받아 해당 지역의 시설인지 확인하도록 변경
    MapResponseDTO.PetFacilityResponseDto getPetFacilityById(Long id, String region_province, String regions_city, String regions_town);

    // 추가: 위치 기반 전체 검색을 위한 메서드
    MapResponseDTO.PetFacilityPageResponseDto getPetFacilitiesByLocation(String region_province, String regions_city, String regions_town, int page, int size);

    Long createPin(MapRequestDTO.PinRequestDto requestDto);

    Long updatePin(Long pinId, MapRequestDTO.PinRequestDto requestDto);

    Long deletePin(Long pinId);

    MapResponseDTO.GetPinDto getPinById(Long Id);

}
