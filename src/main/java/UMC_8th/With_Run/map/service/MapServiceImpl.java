package UMC_8th.With_Run.map.service;

import UMC_8th.With_Run.common.apiResponse.status.ErrorCode;
import UMC_8th.With_Run.common.exception.handler.MapHandler;
import UMC_8th.With_Run.map.dto.MapRequestDTO;
import UMC_8th.With_Run.map.dto.MapResponseDTO;
import UMC_8th.With_Run.map.entity.PetFacility;
import UMC_8th.With_Run.map.entity.Pin;
import UMC_8th.With_Run.map.repository.PetFacilityRepository;
import UMC_8th.With_Run.map.repository.PinRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MapServiceImpl implements MapService {

    private final PetFacilityRepository petFacilityRepository;
    private final PinRepository pinRepository;
    private final CourseService courseService;

    @Override
    public MapResponseDTO.PetFacilityPageResponseDto getPetFacilityByCategory(String category, String region_province, String regions_city, String regions_town, int page, int size) {
        validatePagingParameters(page, size);

        Pageable pageable = PageRequest.of(page, size);

        Page<PetFacility> facilities;

        // "전체" 또는 null인 경우를 처리하여 검색 로직을 단순화
        if ("전체".equals(region_province) || region_province == null) {
            facilities = petFacilityRepository.findByCategoryContainingIgnoreCase(category, pageable);
        } else if ("전체".equals(regions_city) || regions_city == null) {
            facilities = petFacilityRepository.findByCategoryContainingIgnoreCaseAndProvinceContaining(category, region_province, pageable);
        } else if ("전체".equals(regions_town) || regions_town == null) {
            facilities = petFacilityRepository.findByCategoryContainingIgnoreCaseAndProvinceContainingAndCityContaining(category, region_province, regions_city, pageable);
        } else {
            facilities = petFacilityRepository.findByCategoryContainingIgnoreCaseAndProvinceContainingAndCityContainingAndTownContaining(category, region_province, regions_city, regions_town, pageable);
        }


        if (page >= facilities.getTotalPages() && facilities.getTotalPages() > 0) {
            throw new MapHandler(ErrorCode.PAGE_OUT_OF_RANGE);
        }

        List<MapResponseDTO.PetFacilityResponseDto> content = facilities.getContent().stream()
                .map(facility -> MapResponseDTO.PetFacilityResponseDto.builder()
                        .id(facility.getId())
                        .name(facility.getName())
                        .category(facility.getCategory())
                        .longitude(facility.getLongitude())
                        .latitude(facility.getLatitude())
                        .address(facility.getAddress())
                        .closed_day(facility.getClosed_day())
                        .running_time(facility.getRunning_time())
                        .has_parking(facility.getHas_parking())
                        .build())
                .collect(Collectors.toList());

        return MapResponseDTO.PetFacilityPageResponseDto.builder()
                .content(content)
                .totalElements(facilities.getTotalElements())
                .totalPages(facilities.getTotalPages())
                .size(facilities.getSize())
                .number(facilities.getNumber())
                .first(facilities.isFirst())
                .last(facilities.isLast())
                .build();
    }

    private void validatePagingParameters(int page, int size) {
        if (page < 0) {
            throw new MapHandler(ErrorCode.WRONG_PAGE);
        }
        if (size < 1 || size > 100) {
            throw new MapHandler(ErrorCode.WRONG_PAGE_SIZE);
        }
    }

    // getPetFacilityById 메서드 시그니처를 인터페이스에 맞게 수정
    @Override
    public MapResponseDTO.PetFacilityResponseDto getPetFacilityById(Long id, String region_province, String regions_city, String regions_town) {
        PetFacility petFacility = petFacilityRepository.findById(id)
                .orElseThrow(() -> new MapHandler(ErrorCode.BAD_REQUEST));

        return MapResponseDTO.PetFacilityResponseDto.builder()
                .id(petFacility.getId())
                .name(petFacility.getName())
                .category(petFacility.getCategory())
                .longitude(petFacility.getLongitude())
                .latitude(petFacility.getLatitude())
                .address(petFacility.getAddress())
                .closed_day(petFacility.getClosed_day())
                .running_time(petFacility.getRunning_time())
                .has_parking(petFacility.getHas_parking())
                .build();
    }


    @Override
    @Transactional
    public Long createPin(MapRequestDTO.PinRequestDto requestDto) {
        Pin pin = Pin.builder()
                .name(requestDto.getName())
                .detail(requestDto.getDetail())
                .color(requestDto.getColor())
                .latitude(requestDto.getLatitude())
                .longitude(requestDto.getLongitude())
                .createdAt(LocalDateTime.now())
                .build();

        pinRepository.save(pin);

        return pin.getId();
    }

    @Override
    @Transactional
    public Long updatePin(Long pinId, MapRequestDTO.PinRequestDto requestDto) {
        Pin pin = pinRepository.findById(pinId)
                .orElseThrow(() -> new MapHandler(ErrorCode.PIN_NOT_FOUND));
        pin.setName(requestDto.getName());
        pin.setDetail(requestDto.getDetail());
        pin.setColor(requestDto.getColor());
        pin.setLatitude(requestDto.getLatitude());
        pin.setLongitude(requestDto.getLongitude());
        pin.setUpdatedAt(LocalDateTime.now());

        pinRepository.save(pin);
        return pin.getId();
    }

    @Override
    @Transactional
    public Long deletePin(Long pinId) {
        Pin pin = pinRepository.findById(pinId)
                .orElseThrow(() -> new MapHandler(ErrorCode.PIN_NOT_FOUND));

        pinRepository.deleteById(pinId);

        return pin.getId();
    }

    @Override
    @Transactional(readOnly = true)
    public MapResponseDTO.GetPinDto getPinById(Long pinId) {
        Pin pin = pinRepository.findById(pinId)
                .orElseThrow(() -> new MapHandler(ErrorCode.PIN_NOT_FOUND));
        return fromEntity(pin);
    }

    public static MapResponseDTO.GetPinDto fromEntity(Pin pin) {
        return MapResponseDTO.GetPinDto.builder()
                .pinId(pin.getId())
                .courseId(pin.getCourseId())
                .name(pin.getName())
                .detail(pin.getDetail())
                .color(pin.getColor())
                .latitude(pin.getLatitude())
                .longitude(pin.getLongitude())
                .build();
    }

    @Override
    public MapResponseDTO.PetFacilityPageResponseDto getPetFacilitiesByLocation(String region_province, String regions_city, String regions_town, int page, int size) {
        validatePagingParameters(page, size);

        Pageable pageable = PageRequest.of(page, size);
        Page<PetFacility> facilities;

        if ("전체".equals(region_province) || region_province == null) {
            facilities = petFacilityRepository.findAll(pageable);
        } else if ("전체".equals(regions_city) || regions_city == null) {
            facilities = petFacilityRepository.findByProvinceContaining(region_province, pageable);
        } else if ("전체".equals(regions_town) || regions_town == null) {
            facilities = petFacilityRepository.findByProvinceContainingAndCityContaining(region_province, regions_city, pageable);
        } else {
            facilities = petFacilityRepository.findByProvinceContainingAndCityContainingAndTownContaining(region_province, regions_city, regions_town, pageable);
        }


        if (page >= facilities.getTotalPages() && facilities.getTotalPages() > 0) {
            throw new MapHandler(ErrorCode.PAGE_OUT_OF_RANGE);
        }

        List<MapResponseDTO.PetFacilityResponseDto> content = facilities.getContent().stream()
                .map(facility -> MapResponseDTO.PetFacilityResponseDto.builder()
                        .id(facility.getId())
                        .name(facility.getName())
                        .category(facility.getCategory())
                        .longitude(facility.getLongitude())
                        .latitude(facility.getLatitude())
                        .address(facility.getAddress())
                        .closed_day(facility.getClosed_day())
                        .running_time(facility.getRunning_time())
                        .has_parking(facility.getHas_parking())
                        .build())
                .collect(Collectors.toList());

        return MapResponseDTO.PetFacilityPageResponseDto.builder()
                .content(content)
                .totalElements(facilities.getTotalElements())
                .totalPages(facilities.getTotalPages())
                .size(facilities.getSize())
                .number(facilities.getNumber())
                .first(facilities.isFirst())
                .last(facilities.isLast())
                .build();
    }
}