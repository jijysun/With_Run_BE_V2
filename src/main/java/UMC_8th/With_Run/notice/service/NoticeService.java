package UMC_8th.With_Run.notice.service;

import UMC_8th.With_Run.common.apiResponse.status.ErrorCode;
import UMC_8th.With_Run.common.exception.GeneralException;
import UMC_8th.With_Run.course.entity.Course;
import UMC_8th.With_Run.course.repository.CourseRepository;
import UMC_8th.With_Run.notice.dto.NoticeResponse;
import UMC_8th.With_Run.notice.entity.Notice;
import UMC_8th.With_Run.notice.entity.NoticeType;
import UMC_8th.With_Run.notice.repository.NoticeRepository;
import UMC_8th.With_Run.user.entity.User;
import UMC_8th.With_Run.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NoticeService {

    private final NoticeRepository noticeRepository;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;

    private boolean isNoticeOn(Long userId) {
        return userRepository.existsByIdAndNoticeEnabledTrue(userId);
    }

    public void createNotice(User receiver, User actor, Long targetId, NoticeType type) {

        // 수신자 알림 설정 먼저 확인
        if (!isNoticeOn(receiver.getId())) {
            return; // 알림 저장하지 않음
        }
        String actorName = actor.getProfile().getName();
        String courseName = "";

        if (targetId != null) {
            courseName = courseRepository.findById(targetId)
                    .map(Course::getName)
                    .orElse("게시글");
        } else {
            courseName = "";
        }

        String message = generateMessage(type, actorName, courseName);

        Notice notice = new Notice(receiver.getId(), actor.getId(), targetId, message, type);
        noticeRepository.save(notice);
    }


    private String generateMessage(NoticeType type, String actorName, String courseName) {
        return switch (type) {
            case FOLLOW -> actorName + "님이 팔로우하였습니다.";
            case LIKE -> actorName + "님이 '" + courseName + "'에 좋아요를 눌렀습니다.";
            case SCRAP -> actorName + "님이 '" + courseName + "'을(를) 스크랩하였습니다.";
        };
    }

    @Transactional
    public List<NoticeResponse> getUserNotices(Long userId) {
        List<Notice> notices = noticeRepository.findByReceiverId(userId);

        // User 엔티티 한번씩 조회하는걸 최소화하려면 Map 등 캐싱 고려 가능
        return notices.stream().map(notice -> {
            User receiver = userRepository.findById(notice.getReceiverId())
                    .orElseThrow(() -> new GeneralException(ErrorCode.USER_NOT_FOUND));
            User actor = userRepository.findById(notice.getActorId())
                    .orElseThrow(() -> new GeneralException(ErrorCode.USER_NOT_FOUND));

            String courseName = "";
            if (notice.getTargetId() != null) {
                courseName = courseRepository.findById(notice.getTargetId())
                        .map(Course::getName)
                        .orElse("");
            }

            return new NoticeResponse(
                    notice.getId(),
                    notice.getMessage(),
                    receiver.getId(),
                    receiver.getProfile().getName(),
                    actor.getId(),
                    actor.getProfile().getName(),
                    notice.getType(),
                    notice.getTargetId(),
                    courseName

            );
        }).collect(Collectors.toList());
    }
}
