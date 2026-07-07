package UMC_8th.With_Run.course.service;

import UMC_8th.With_Run.course.entity.Course;
import UMC_8th.With_Run.course.repository.CourseRepository;
import UMC_8th.With_Run.notice.entity.NoticeType;
import UMC_8th.With_Run.notice.service.NoticeService;
import UMC_8th.With_Run.user.entity.Scraps;
import UMC_8th.With_Run.user.entity.User;
import UMC_8th.With_Run.user.repository.ScrapsRepository;
import UMC_8th.With_Run.user.repository.UserRepository;
import UMC_8th.With_Run.user.service.ScrapService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ScrapCourseService {

    private final ScrapsRepository scrapsRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final NoticeService noticeService;

    @Transactional
    public void scrapCourse(Long userId, Long courseId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 산책 코스입니다."));

        if (scrapsRepository.existsByUserAndCourse(user, course)) {
            throw new IllegalStateException("이미 스크랩한 코스입니다.");
        }

        Scraps scrap = new Scraps(user, course);
        scrapsRepository.save(scrap);
        noticeService.createNotice(course.getUser(), user, courseId, NoticeType.SCRAP);
    }

    @Transactional
    public void unscrapCourse(Long userId, Long courseId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 산책 코스입니다."));

        Scraps scrap = scrapsRepository.findByUserAndCourse(user, course)
                .orElseThrow(() -> new IllegalStateException("스크랩한 기록이 없습니다."));

        scrapsRepository.delete(scrap);
    }
}
