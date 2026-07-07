package UMC_8th.With_Run.map.service;


import UMC_8th.With_Run.map.dto.MapRequestDTO; // DTO 패키지는 유지
import org.springframework.web.multipart.MultipartFile;

public interface CourseService {
    Long createCourse(Long userId, MapRequestDTO.CourseCreateRequestDto requestDto);
    // userId 파라미터가 없는 메서드는 제거 (JWT에서 자동 추출)
    Long createCourseV2(Long userId, MapRequestDTO.CourseCreateRequestDto requestDto, MultipartFile courseImageFile);
}