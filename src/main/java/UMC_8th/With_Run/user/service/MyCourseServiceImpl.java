package UMC_8th.With_Run.user.service;

import UMC_8th.With_Run.common.apiResponse.status.ErrorCode;
import UMC_8th.With_Run.common.exception.GeneralException;
import UMC_8th.With_Run.common.exception.handler.UserHandler;
import UMC_8th.With_Run.common.security.jwt.JwtTokenProvider;
import UMC_8th.With_Run.course.entity.Course;
import UMC_8th.With_Run.course.repository.CourseRepository;
import UMC_8th.With_Run.map.entity.Pin;
import UMC_8th.With_Run.map.entity.RegionProvince;
import UMC_8th.With_Run.map.entity.RegionsCity;
import UMC_8th.With_Run.map.entity.RegionsTown;
import UMC_8th.With_Run.map.repository.PinRepository;
import UMC_8th.With_Run.user.dto.UserRequestDto;
import UMC_8th.With_Run.user.dto.UserRequestDto.UpdateCourseDTO;
import UMC_8th.With_Run.user.dto.UserResponseDto.MyCourseItemDTO;
import UMC_8th.With_Run.user.dto.UserResponseDto.MyCourseListResultDTO;
import UMC_8th.With_Run.user.entity.User;
import UMC_8th.With_Run.user.repository.RegionCityRepository;
import UMC_8th.With_Run.user.repository.RegionProvinceRepository;
import UMC_8th.With_Run.user.repository.RegionTownRepository;
import UMC_8th.With_Run.user.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MyCourseServiceImpl implements MyCourseService {

    private final ObjectMapper objectMapper;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final PinRepository pinRepository;
    private final RegionProvinceRepository provinceRepository;
    private final RegionCityRepository cityRepository;
    private final RegionTownRepository townRepository;

    @Override
    public MyCourseListResultDTO getMyCourses(HttpServletRequest request) {
        Authentication authentication = jwtTokenProvider.extractAuthentication(request);
        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserHandler(ErrorCode.WRONG_USER));

        List<Course> myCourses = courseRepository.findAllByUserId(user.getId());

        List<MyCourseItemDTO> courseItems = myCourses.stream()
                .map(course -> MyCourseItemDTO.builder()
                        .courseId(course.getId())
                        .courseName(course.getName())
                        .keyword(course.getKeyWord())
                        .time(course.getTime())
                        .courseImage(course.getCourseImage())
                        .location(course.getLocation())
                        .createdAt(course.getCreatedAt())
                        .build())
                .toList();

        return MyCourseListResultDTO.builder()
                .myCourseList(courseItems)
                .build();
    }

    @Transactional
    @Override
    public UserRequestDto.UpdateCourseDTO updateCourse(Long courseId, UpdateCourseDTO dto) {

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new UserHandler(ErrorCode.WRONG_USER));

        // 1. 코스 정보 수정
        course.setName(dto.getName());
        course.setDescription(dto.getDescription());
        course.setTime(dto.getTime());
        course.setKeyWord(convertToJson(dto.getKeyWords()));
        course.setOverviewPolyline(dto.getOverviewPolyline());

        // 2. 지역 정보 수정
        RegionProvince province = provinceRepository.findById(dto.getProvinceId())
                .orElseThrow(() -> new GeneralException(ErrorCode.BAD_REQUEST));
        RegionsCity city = null;
        if (dto.getCityId() != null) {
            city = cityRepository.findById(dto.getCityId())
                    .orElseThrow(() -> new GeneralException(ErrorCode.BAD_REQUEST));
        }
        RegionsTown town = null;
        if (dto.getTownId() != null) {
            town = townRepository.findById(dto.getTownId())
                    .orElseThrow(() -> new GeneralException(ErrorCode.BAD_REQUEST));
        }

        course.setRegionProvince(province);
        course.setRegionsCity(city);
        course.setRegionsTown(town);

        // 3. 핀 정보 업데이트
        // 3-1. 기존에 코스와 연관된 핀들을 모두 삭제합니다.
        // PinRepository에 findAllByCourse(Course course) 메서드가 필요합니다.
        List<Pin> existingPins = pinRepository.findAllByCourse(course);
        pinRepository.deleteAll(existingPins);

        // 3-2. DTO에 담겨온 핀 정보를 바탕으로 새로운 Pin 엔티티를 생성하고 저장합니다.
        if (dto.getPins() != null && !dto.getPins().isEmpty()) {
            List<Pin> newPins = IntStream.range(0, dto.getPins().size())
                    .mapToObj(i -> {
                        // DTO에서 i번째 PinRequest를 가져옵니다.
                        UserRequestDto.PinRequest pinResponse = dto.getPins().get(i);
                        // Pin 엔티티를 생성하고 pinOrder를 설정합니다.
                        return Pin.builder()
                                .name(pinResponse.getName())
                                .color(pinResponse.getColor())
                                .latitude(pinResponse.getLatitude())
                                .longitude(pinResponse.getLongitude())
                                .detail(pinResponse.getDetail())
                                .pinOrder(i + 1)
                                .course(course)
                                .build();
                    })
                    .toList();

            pinRepository.saveAll(newPins); // 생성된 핀 목록을 한 번에 저장합니다.
        }
        // 저장은 @Transactional 로 자동 반영

        return dto;
    }

    private String convertToJson(List<String> list) {
        try {
            return objectMapper.writeValueAsString(list != null ? list : Collections.emptyList());
        } catch (JsonProcessingException e) {
            throw new GeneralException(ErrorCode.BAD_REQUEST);
        }
    }
}
