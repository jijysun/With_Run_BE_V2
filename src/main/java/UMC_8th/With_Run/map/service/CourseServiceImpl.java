package UMC_8th.With_Run.map.service;

import UMC_8th.With_Run.common.apiResponse.status.ErrorCode;
import UMC_8th.With_Run.common.config.s3.S3Uploader;
import UMC_8th.With_Run.common.exception.handler.MapHandler;
import UMC_8th.With_Run.course.entity.Course;
import UMC_8th.With_Run.course.repository.CourseRepository;
import UMC_8th.With_Run.map.dto.MapRequestDTO;
import UMC_8th.With_Run.map.entity.Pin;
import UMC_8th.With_Run.map.entity.RegionProvince;
import UMC_8th.With_Run.map.entity.RegionsCity;
import UMC_8th.With_Run.map.entity.RegionsTown;
import UMC_8th.With_Run.map.repository.PinRepository;
import UMC_8th.With_Run.map.repository.RegionsTownRepository;
import UMC_8th.With_Run.user.repository.RegionProvinceRepository;
import UMC_8th.With_Run.map.repository.RegionsCityRepository;
import UMC_8th.With_Run.user.entity.User;
import UMC_8th.With_Run.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CourseServiceImpl implements CourseService {

    private final CourseRepository courseRepository;
    private final PinRepository pinRepository;
    private final RegionProvinceRepository regionProvinceRepository;
    private final RegionsCityRepository regionsCityRepository;
    private final RegionsTownRepository regionsTownRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final S3Uploader s3Uploader;

    @Override
    @Transactional
    public Long createCourse(Long userId, MapRequestDTO.CourseCreateRequestDto requestDto) {

        // 키워드를 JSON 형태로 변환
        String keywordsJson;
        try {
            keywordsJson = objectMapper.writeValueAsString(requestDto.getKeywords());
        } catch (Exception e) {
            throw new MapHandler(ErrorCode.BAD_REQUEST);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new MapHandler(ErrorCode.USER_NOT_FOUND));

        RegionProvince regionProvince = regionProvinceRepository.findById(requestDto.getRegionProvinceId())
                .orElseThrow(() -> new MapHandler(ErrorCode.REGION_PROVINCE_NOT_FOUND));

        RegionsCity regionsCity = regionsCityRepository.findById(requestDto.getRegionsCityId())
                .orElseThrow(() -> new MapHandler(ErrorCode.REGION_CITY_NOT_FOUND));

        // RegionsTown 처리 - 선택사항이므로 null 체크
        RegionsTown regionsTown = null;
        if (requestDto.getRegionsTownId() != null) {
            regionsTown = regionsTownRepository.findById(requestDto.getRegionsTownId())
                    .orElseThrow(() -> new MapHandler(ErrorCode.REGION_CITY_NOT_FOUND));
        }

        // Course 엔티티 생성
        Course course = Course.builder()
                .name(requestDto.getName())
                .description(requestDto.getDescription())
                .keyWord(keywordsJson)
                .time(requestDto.getTime())
                .user(user)
                .createdAt(LocalDateTime.now())
                .regionProvince(regionProvince)
                .regionsCity(regionsCity)
                .regionsTown(regionsTown)
                .overviewPolyline(requestDto.getOverviewPolyline())
                .build();

        // 1. 코스를 먼저 저장하여 ID를 할당받습니다.
        Course savedCourse = courseRepository.save(course);

        // 2. DTO에 담긴 핀 객체 리스트를 이용하여 실제 Pin 엔티티를 생성합니다.
        List<Pin> pins = new ArrayList<>();
        for (MapRequestDTO.PinRequestDto pinDto : requestDto.getPins()) {
            Pin newPin = Pin.builder()
                    .name(pinDto.getName())
                    .detail(pinDto.getDetail())
                    .color(pinDto.getColor())
                    .latitude(pinDto.getLatitude())
                    .longitude(pinDto.getLongitude())
                    .pinOrder(pinDto.getPinOrder()) // DTO의 pinOrder 값을 사용
                    .course(savedCourse) // Course 엔티티를 직접 설정
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            pins.add(newPin);
        }

        // 3. 생성된 핀 엔티티 리스트를 한 번에 저장합니다.
        pinRepository.saveAll(pins);

        // regionsData를 활용한 로직이 필요하다면 여기에 추가
        // 예: 코스와 지역 간의 관계를 관리하는 중간 테이블에 저장하는 로직
        // List<RegionsCity> regions = regionsCityRepository.findAllById(
        //     requestDto.getRegionsData().stream().map(MapRequestDTO.RegionRequestDto::getId).collect(Collectors.toList())
        // );
        // savedCourse.setRegions(regions);

        return savedCourse.getId();
    }

    @Override
    @Transactional
    public Long createCourseV2(Long userId, MapRequestDTO.CourseCreateRequestDto requestDto, MultipartFile courseImageFile) {

        String keywordsJson;
        try {
            keywordsJson = objectMapper.writeValueAsString(requestDto.getKeywords());
        } catch (Exception e) {
            throw new MapHandler(ErrorCode.BAD_REQUEST);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new MapHandler(ErrorCode.USER_NOT_FOUND));

        RegionProvince regionProvince = regionProvinceRepository.findById(requestDto.getRegionProvinceId())
                .orElseThrow(() -> new MapHandler(ErrorCode.REGION_PROVINCE_NOT_FOUND));

        RegionsCity regionsCity = null;
        if (requestDto.getRegionsCityId() != null) {
            regionsCity = regionsCityRepository.findById(requestDto.getRegionsCityId())
                    .orElseThrow(() -> new MapHandler(ErrorCode.REGION_CITY_NOT_FOUND));
        }

        // RegionsTown 처리 - 선택사항이므로 null 체크
        RegionsTown regionsTown = null;
        if (requestDto.getRegionsTownId() != null) {
            regionsTown = regionsTownRepository.findById(requestDto.getRegionsTownId())
                    .orElseThrow(() -> new MapHandler(ErrorCode.REGION_TOWN_NOT_FOUND));
        }

        String courseImageUrl = null;
        if (courseImageFile != null && !courseImageFile.isEmpty()) {
            try {
                courseImageUrl = s3Uploader.upload(courseImageFile, "courses");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        Course course = Course.builder()
                .name(requestDto.getName())
                .description(requestDto.getDescription())
                .keyWord(keywordsJson)
                .time(requestDto.getTime())
                .user(user)
                .createdAt(LocalDateTime.now())
                .regionProvince(regionProvince)
                .regionsCity(regionsCity)
                .regionsTown(regionsTown)
                .overviewPolyline(requestDto.getOverviewPolyline())
                .courseImage(courseImageUrl)
                .build();

        Course savedCourse = courseRepository.save(course);

        List<Pin> pins = new ArrayList<>();
        for (MapRequestDTO.PinRequestDto pinDto : requestDto.getPins()) {
            Pin newPin = Pin.builder()
                    .name(pinDto.getName())
                    .detail(pinDto.getDetail())
                    .color(pinDto.getColor())
                    .latitude(pinDto.getLatitude())
                    .longitude(pinDto.getLongitude())
                    .pinOrder(pinDto.getPinOrder())
                    .course(savedCourse)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            pins.add(newPin);
        }

        pinRepository.saveAll(pins);

        return savedCourse.getId();
    }
}