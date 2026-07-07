package UMC_8th.With_Run.course.service;

import UMC_8th.With_Run.course.dto.CourseDetailResponse;
import UMC_8th.With_Run.course.dto.CourseDetailResponse.PinResponse;
import UMC_8th.With_Run.course.entity.Course;
import UMC_8th.With_Run.map.entity.Pin;
import UMC_8th.With_Run.course.repository.CourseRepository;
import UMC_8th.With_Run.map.repository.PinRepository;
import UMC_8th.With_Run.user.entity.Likes;
import UMC_8th.With_Run.user.entity.Scraps;
import UMC_8th.With_Run.user.entity.User;
import UMC_8th.With_Run.user.repository.LikesRepository;
import UMC_8th.With_Run.user.repository.ScrapsRepository;
import UMC_8th.With_Run.user.repository.UserRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourseDetailService {

    private final CourseRepository courseRepository;
    private final PinRepository pinRepository;
    private final LikesRepository likesRepository;
    private final ScrapsRepository scrapsRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional(readOnly = true)
    public CourseDetailResponse getCourseDetail(Long courseId, Long userId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("해당 코스를 찾을 수 없습니다."));

        List<String> keywords;
        try {
            keywords = objectMapper.readValue(course.getKeyWord(), new TypeReference<List<String>>() {});
        } catch (Exception e) {
            keywords = Collections.emptyList();
        }

        // 핀을 pinOrder 오름차순으로 정렬해서 조회
        List<Pin> pins = pinRepository.findByCourseOrderByPinOrderAsc(course);

        List<CourseDetailResponse.PinResponse> pinResponses = pins.stream()
                .map(pin -> CourseDetailResponse.PinResponse.builder()
                        .id(pin.getId())
                        .name(pin.getName())
                        .color(pin.getColor())
                        .latitude(pin.getLatitude())
                        .longitude(pin.getLongitude())
                        .detail(pin.getDetail())
                        .build())
                .collect(Collectors.toList());

        // 좋아요 여부 확인
        Boolean isLiked = false;
        if (userId != null) {
            try {
                User user = userRepository.findById(userId).orElse(null);
                if (user != null) {
                    isLiked = likesRepository.existsByUserAndCourse(user, course);
                }
            } catch (Exception e) {
                // 에러 발생 시 false로 처리
                isLiked = false;
            }
        }

        // 스크랩 여부 확인
        Boolean isScrapped = false;
        if (userId != null) {
            try {
                User user = userRepository.findById(userId).orElse(null);
                if (user != null) {
                    isScrapped = scrapsRepository.existsByUserAndCourse(user, course);
                }
            } catch (Exception e) {
                // 에러 발생 시 false로 처리
                isScrapped = false;
            }
        }

        return CourseDetailResponse.builder()
                .id(course.getId())
                .name(course.getName())
                .imageUrl(course.getCourseImage())
                .description(course.getDescription())
                .keywords(keywords)
                .time(course.getTime())
                .pins(pinResponses)
                .overviewPolyline(course.getOverviewPolyline()) // 코스 전체 경로 추가
                .isLiked(isLiked) // 좋아요 여부 추가
                .isScrapped(isScrapped) // 스크랩 여부 추가
                .build();
    }
}
