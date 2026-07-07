package UMC_8th.With_Run.course.service;

import UMC_8th.With_Run.course.dto.CourseResponse;
import UMC_8th.With_Run.course.entity.Course;
import UMC_8th.With_Run.course.repository.CourseRepository;
import UMC_8th.With_Run.user.repository.LikesRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NearbyCourseService {

    private final CourseRepository courseRepository;
    private final LikesRepository likesRepository;
    private final ObjectMapper objectMapper = new ObjectMapper(); // ✅ JSON 파서

    public List<CourseResponse> nearbyCourse(Long provinceId, Long cityId, Long townId) {
        List<Course> courses = courseRepository.findCoursesByRegion(provinceId, cityId, townId);

        if (courses.isEmpty()) {
            return List.of();
        }

        List<Long> courseIds = courses.stream()
                .map(Course::getId)
                .collect(Collectors.toList());

        Map<Long, Long> likeCountMap = likesRepository.getLikeCountsByCourseIds(courseIds).stream()
                .collect(Collectors.toMap(
                        arr -> ((Number) arr[0]).longValue(),
                        arr -> ((Number) arr[1]).longValue()
                ));

        List<Course> sortedCourses = courses.stream()
                .sorted(Comparator.comparing((Course c) -> likeCountMap.getOrDefault(c.getId(), 0L)).reversed())
                .collect(Collectors.toList());

        return sortedCourses.stream()
                .map(course -> {
                    List<String> parsedKeywords = new ArrayList<>();
                    try {
                        parsedKeywords = objectMapper.readValue(course.getKeyWord(), new TypeReference<List<String>>() {});
                    } catch (Exception e) {
                        System.err.println("키워드 파싱 오류: " + e.getMessage());
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
                            .location(fullLocation)
                            .build();
                })
                .collect(Collectors.toList());
    }

    public List<CourseResponse> nearbyCoursePreview(Long provinceId, Long cityId, Long townId) {
        return nearbyCourse(provinceId, cityId, townId).stream()
                .limit(10) // 프리뷰는 상위 10개
                .collect(Collectors.toList());
    }

    public List<CourseResponse> nearbyCourseSearchResult(Long provinceId, Long cityId, Long townId, String keyword) {
        return nearbyCourse(provinceId, cityId, townId).stream()
                .filter(course -> {
                    if (keyword == null || keyword.isBlank()) return true;

                    boolean nameMatch = course.getName().contains(keyword);
                    boolean keywordMatch = course.getKeyword() != null &&
                            course.getKeyword().stream().anyMatch(k -> k.contains(keyword));
                    boolean locationMatch = course.getLocation() != null && course.getLocation().contains(keyword);

                    return nameMatch || keywordMatch || locationMatch;
                })
                .collect(Collectors.toList());
    }
}
