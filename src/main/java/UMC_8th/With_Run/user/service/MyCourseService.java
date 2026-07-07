package UMC_8th.With_Run.user.service;

import UMC_8th.With_Run.user.dto.UserRequestDto.UpdateCourseDTO;
import UMC_8th.With_Run.user.dto.UserResponseDto.MyCourseListResultDTO;
import jakarta.servlet.http.HttpServletRequest;

public interface MyCourseService {
    MyCourseListResultDTO getMyCourses(HttpServletRequest request);
    UpdateCourseDTO updateCourse(Long courseId, UpdateCourseDTO dto);

}
