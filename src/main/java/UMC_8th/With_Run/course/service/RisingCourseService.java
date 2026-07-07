package UMC_8th.With_Run.course.service;

import UMC_8th.With_Run.course.dto.CourseResponse;
import UMC_8th.With_Run.course.entity.Course;
import UMC_8th.With_Run.course.repository.CourseRepository;
import UMC_8th.With_Run.user.repository.LikesRepository;
import UMC_8th.With_Run.user.repository.ScrapsRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RisingCourseService {

    private final CourseRepository courseRepository;
    private final LikesRepository likesRepository;
    private final ScrapsRepository scrapsRepository;
    private final ObjectMapper objectMapper = new ObjectMapper(); // ✅ JSON 파서

    public List<CourseResponse> risingCourse() {
        List<Course> allCourses = courseRepository.findAll();

        if (allCourses.isEmpty()) return List.of();

        List<Long> courseIds = allCourses.stream()
                .map(Course::getId)
                .collect(Collectors.toList());

        // 좋아요 개수
        Map<Long, Long> likeCountMap = likesRepository.getLikeCountsByCourseIds(courseIds).stream()
                .collect(Collectors.toMap(
                        arr -> ((Number) arr[0]).longValue(),
                        arr -> ((Number) arr[1]).longValue()
                ));

        // 스크랩 개수
        Map<Long, Long> scrapCountMap = scrapsRepository.getScrapCountsByCourseIds(courseIds).stream()
                .collect(Collectors.toMap(
                        arr -> ((Number) arr[0]).longValue(),
                        arr -> ((Number) arr[1]).longValue()
                ));

        // 조건에 해당하는 코스만 필터링
        List<Course> filtered = allCourses.stream()
                .filter(course -> {
                    long likeCount = likeCountMap.getOrDefault(course.getId(), 0L);
                    long scrapCount = scrapCountMap.getOrDefault(course.getId(), 0L);
                    return likeCount >= 10 || scrapCount >= 10;
                })
                .limit(50)
                .collect(Collectors.toList());

        // 키워드 파싱 + 응답 매핑
        return filtered.stream()
                .map(course -> {
                    List<String> parsedKeywords = new ArrayList<>();
                    try {
                        parsedKeywords = objectMapper.readValue(course.getKeyWord(), new TypeReference<List<String>>() {});
                    } catch (Exception e) {
                        System.err.println("키워드 파싱 오류 (떠오르는): " + e.getMessage());
                    }

                    String fullLocation = "";
                    if (course.getRegionProvince() != null) fullLocation += course.getRegionProvince().getName() + " ";
                    if (course.getRegionsCity() != null) fullLocation += course.getRegionsCity().getName() + " ";
                    if (course.getRegionsTown() != null) fullLocation += course.getRegionsTown().getName();

                    return CourseResponse.builder()
                            .courseId(course.getId())
                            .name(course.getName())
                            .keyword(parsedKeywords)
                            .time(course.getTime())
                            .courseImage(course.getCourseImage())
                            .location(fullLocation.trim())
                            .build();
                })
                .collect(Collectors.toList());
    }

    // 떠오르는 산책코스 Preview (상위 10개만)
    public List<CourseResponse> risingCoursePreview() {
        // risingCourse() 호출해서 상위 50개 가져오고, 그 중 10개만 반환
        return risingCourse().stream()
                .limit(10)
                .collect(Collectors.toList());
    }

    public List<CourseResponse> risingCourseSearchResult(String keyword) {
        return risingCourse().stream()
                .filter(course -> {
                    if (keyword == null || keyword.isBlank()) return true;

                    boolean nameMatch = course.getName().contains(keyword);
                    boolean keywordMatch = course.getKeyword().stream().anyMatch(k -> k.contains(keyword));
                    boolean locationMatch = course.getLocation() != null && course.getLocation().contains(keyword);

                    return nameMatch || keywordMatch || locationMatch;
                })
                .collect(Collectors.toList());
    }
}
