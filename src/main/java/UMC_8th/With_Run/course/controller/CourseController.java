package UMC_8th.With_Run.course.controller;

import UMC_8th.With_Run.common.exception.GeneralException;
import UMC_8th.With_Run.common.security.jwt.JwtTokenProvider;
import UMC_8th.With_Run.course.dto.CourseDetailResponse;
import UMC_8th.With_Run.course.dto.CourseResponse;
import UMC_8th.With_Run.common.apiResponse.status.ErrorCode;
import UMC_8th.With_Run.course.repository.CourseRepository;
import UMC_8th.With_Run.course.service.*;
import UMC_8th.With_Run.user.entity.User;
import UMC_8th.With_Run.user.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.nio.file.attribute.UserPrincipal;
import java.util.List;
import java.util.Map;

@Tag(name = "산책코스 API", description = "우리동네 산책코스, 떠오르는 산책코스, 검색, 스크랩 및 좋아요 등 산책코스 관련 기능 제공")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/course")
@Tag(name = "산책 코스 API")
public class CourseController {

    private final NearbyCourseService nearbyCourseService;
    private final RisingCourseService risingCourseService;
    private final CourseDetailService courseDetailService;
    private final LikeCourseService likeCourseService;
    private final ScrapCourseService scrapCourseService;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;

    @Operation(summary = "우리동네 산책 코스 프리뷰 조회", description = "선택한 지역에 해당하는 산책 코스를 최대 10개까지 미리보기로 조회합니다.")
    @GetMapping("/nearby/preview")
    public List<CourseResponse> getNearbyCoursePreview(
            @RequestParam Long provinceId,
            @RequestParam(value = "cityId", required = false) Long cityId,
            @RequestParam(value = "townId", required = false) Long townId
    ){
        return nearbyCourseService.nearbyCoursePreview(provinceId, cityId, townId);
    }

    @Operation(summary = "우리동네 산책 코스 전체 조회", description = "선택한 지역에 해당하는 모든 산책 코스를 좋아요 순으로 조회합니다.")
    @GetMapping("/nearby")
    public List<CourseResponse> getNearbyCourse(
            @RequestParam Long provinceId,
            @RequestParam(value = "cityId", required = false) Long cityId,
            @RequestParam(value = "townId", required = false) Long townId
    ) {
        return nearbyCourseService.nearbyCourse(provinceId, cityId, townId);
    }

    @Operation(summary = "우리동네 산책 코스 검색", description = "선택한 지역의 산책 코스 중 키워드에 해당하는 산책 코스를 검색합니다.")
    @GetMapping("/nearby/search")
    public List<CourseResponse> searchNearbyCourse(
            @RequestParam Long provinceId,
            @RequestParam(value = "cityId", required = false) Long cityId,
            @RequestParam(value = "townId", required = false) Long townId,
            @RequestParam(required = false) String keyword
    ) {
        return nearbyCourseService.nearbyCourseSearchResult(provinceId, cityId, townId,keyword);
    }

    @Operation(summary = "떠오르는 산책 코스 프리뷰 조회", description = "좋아요 또는 스크랩 수가 10개 이상인 떠오르는 산책 코스를 최대 10개까지 미리보기로 조회합니다.")
    @GetMapping("/rising/preview")
    public List<CourseResponse> getRisingCoursePreview() {
        return risingCourseService.risingCoursePreview();
    }

    @Operation(summary = "떠오르는 산책 코스 전체 조회", description = "좋아요 또는 스크랩 수가 10개 이상인 떠오르는 산책 코스를 최대 50개까지 조회합니다.")
    @GetMapping("/rising")
    public List<CourseResponse> getRisingCourse(
    ) {
        return risingCourseService.risingCourse();
    }

    @Operation(summary = "떠오르는 산책 코스 검색", description = "떠오르는 산책 코스 중 키워드에 해당하는 코스를 검색합니다.")
    @GetMapping("/rising/search")
    public List<CourseResponse> searchRisingCourse(
            @RequestParam(required = false) String keyword
    ) {
        return risingCourseService.risingCourseSearchResult(keyword);
    }

    @Operation(summary = "산책 코스 좋아요", description = "사용자가 특정 산책 코스에 좋아요를 누릅니다.")
    @PostMapping("/like")
    public ResponseEntity<?> like(@RequestParam Long courseId,
                                  HttpServletRequest request) {
        Authentication authentication = jwtTokenProvider.extractAuthentication(request);
        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new GeneralException(ErrorCode.WRONG_USER));
        Long userId = user.getId();

        likeCourseService.likeCourse(userId, courseId);

        return ResponseEntity.ok(Map.of("message", "좋아요를 눌렀습니다", "courseId", courseId));
    }

    @Operation(summary = "산책 코스 좋아요 취소", description = "사용자가 특정 산책 코스에 눌렀던 좋아요를 취소합니다.")
    @DeleteMapping("/like")
    public ResponseEntity<?> unlike(@RequestParam Long courseId,
                                    HttpServletRequest request) {
        Authentication authentication = jwtTokenProvider.extractAuthentication(request);
        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new GeneralException(ErrorCode.WRONG_USER));
        Long userId = user.getId();

        likeCourseService.unlikeCourse(userId, courseId);

        return ResponseEntity.ok(Map.of("message", "좋아요를 취소했습니다", "courseId", courseId));
    }

    @Operation(summary = "산책 코스 스크랩", description = "사용자가 특정 산책 코스를 스크랩합니다.")
    @PostMapping("/scrap")
    public ResponseEntity<?> scrap(@RequestParam Long courseId, HttpServletRequest request) {
        Authentication authentication = jwtTokenProvider.extractAuthentication(request);
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new GeneralException(ErrorCode.WRONG_USER));

        scrapCourseService.scrapCourse(user.getId(), courseId);

        return ResponseEntity.ok(Map.of("message", "스크랩 완료", "courseId", courseId));
    }

    @Operation(summary = "산책 코스 스크랩 취소", description = "사용자가 특정 산책 코스 스크랩을 취소합니다.")
    @DeleteMapping("/scrap")
    public ResponseEntity<?> unscrap(@RequestParam Long courseId, HttpServletRequest request) {
        Authentication authentication = jwtTokenProvider.extractAuthentication(request);
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new GeneralException(ErrorCode.WRONG_USER));

        scrapCourseService.unscrapCourse(user.getId(), courseId);

        return ResponseEntity.ok(Map.of("message", "스크랩 취소 완료", "courseId", courseId));
    }

    @Operation(summary = "산책 코스 상세", description = "특정 산책 코스의 상세 정보를 조회합니다.")
    @GetMapping("/detail")
    public ResponseEntity<CourseDetailResponse> getCourseDetail(
            @RequestParam Long courseId,
            HttpServletRequest request) {
        // 사용자 인증 정보 추출 (로그인하지 않은 경우 null)
        Authentication authentication = null;
        try {
            authentication = jwtTokenProvider.extractAuthentication(request);
        } catch (Exception e) {
            // 로그인하지 않은 사용자
        }
        
        // 사용자 ID 추출 (로그인하지 않은 경우 null)
        Long userId = null;
        if (authentication != null) {
            try {
                String email = authentication.getName();
                User user = userRepository.findByEmail(email)
                        .orElse(null);
                if (user != null) {
                    userId = user.getId();
                }
            } catch (Exception e) {
                // 사용자 정보 추출 실패
            }
        }
        
        CourseDetailResponse response = courseDetailService.getCourseDetail(courseId, userId);
        return ResponseEntity.ok(response);
    }






}
