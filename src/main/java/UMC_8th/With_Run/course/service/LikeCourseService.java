package UMC_8th.With_Run.course.service;

import UMC_8th.With_Run.common.exception.GeneralException;
import UMC_8th.With_Run.course.entity.Course;
import UMC_8th.With_Run.course.repository.CourseRepository;
import UMC_8th.With_Run.notice.entity.NoticeType;
import UMC_8th.With_Run.notice.service.NoticeService;
import UMC_8th.With_Run.user.entity.Likes;
import UMC_8th.With_Run.user.entity.User;
import UMC_8th.With_Run.user.repository.LikesRepository;
import UMC_8th.With_Run.user.repository.UserRepository;
import UMC_8th.With_Run.user.service.UserService;
import com.amazonaws.services.kms.model.NotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LikeCourseService {

    private final LikesRepository likesRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final NoticeService noticeService;
    @Transactional
    public void likeCourse(Long userId, Long courseId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 산책 코스입니다."));

        if (likesRepository.existsByUserAndCourse(user, course)) {
            throw new IllegalStateException("이미 좋아요를 누른 코스입니다.");
        }

        Likes likes = new Likes(user, course);
        likesRepository.save(likes);
        noticeService.createNotice(user, course.getUser(), courseId, NoticeType.LIKE);
    }

    @Transactional
    public void unlikeCourse(Long userId, Long courseId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 산책 코스입니다."));

        Likes like = likesRepository.findByUserAndCourse(user, course)
                .orElseThrow(() -> new IllegalStateException("좋아요를 누른 기록이 없습니다."));

        likesRepository.delete(like);

    }
}
