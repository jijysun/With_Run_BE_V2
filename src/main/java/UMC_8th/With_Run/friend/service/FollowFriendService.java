package UMC_8th.With_Run.friend.service;

import UMC_8th.With_Run.notice.entity.NoticeType;
import UMC_8th.With_Run.notice.service.NoticeService;
import UMC_8th.With_Run.user.entity.Follow;
import UMC_8th.With_Run.user.entity.User;
import UMC_8th.With_Run.friend.repository.FollowFriendRepository;
import UMC_8th.With_Run.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class FollowFriendService {

    private final FollowFriendRepository followFriendRepository;
    private final UserRepository userRepository;
    private final NoticeService noticeService;

    @Transactional
    public void followUser(Long userId, Long targetUserId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저입니다: " + userId));
        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 타겟 유저입니다: " + targetUserId));

        // 자기 자신 팔로우 방지
        if (user.getId().equals(targetUser.getId())) {
            throw new IllegalArgumentException("자기 자신을 팔로우할 수 없습니다.");
        }

        // 중복 팔로우 방지
        if (followFriendRepository.existsByUserAndTargetUser(user, targetUser)) {
            throw new IllegalStateException("이미 팔로우한 사용자입니다.");
        }

        Follow follow = Follow.builder()
                .user(user)
                .targetUser(targetUser)
                .createdAt(LocalDate.now())
                .updatedAt(LocalDate.now())
                .build();

        followFriendRepository.save(follow);
        noticeService.createNotice(targetUser, user, null, NoticeType.FOLLOW);

    }

}
