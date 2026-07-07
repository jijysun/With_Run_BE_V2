package UMC_8th.With_Run.friend.service;

import UMC_8th.With_Run.friend.repository.BlockFriendRepository;
import UMC_8th.With_Run.friend.repository.FollowFriendRepository;
import UMC_8th.With_Run.user.entity.Block;
import UMC_8th.With_Run.user.entity.User;
import UMC_8th.With_Run.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class BlockFriendService {

    private final BlockFriendRepository blockFriendRepository;
    private final FollowFriendRepository followRepository;
    private final UserRepository userRepository;

    @Transactional
    public void blockUser(Long userId, Long targetUserId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("차단 유저 조회 실패"));
        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("차단 대상 유저 조회 실패"));

        // 자기 자신 제외
        if (user.getId().equals(targetUser.getId())) {
            throw new IllegalArgumentException("자기 자신은 차단할 수 없습니다.");
        }

        // 이미 차단했는지 확인
        if (blockFriendRepository.existsByUserIdAndBlockTargetUserId(userId, targetUserId)) {
            throw new IllegalStateException("이미 차단한 사용자입니다.");
        }

        // 팔로우 관계 제거
        followRepository.findAll().stream()
                .filter(f -> (f.getUser().equals(user) && f.getTargetUser().equals(targetUser)) ||
                        (f.getUser().equals(targetUser) && f.getTargetUser().equals(user)))
                .forEach(followRepository::delete);

        // 차단 저장
        Block block = Block.builder()
                .userId(user.getId())
                .blockTargetUserId(targetUser.getId())
                .createdAt(LocalDate.now().atStartOfDay())
                .updatedAt(LocalDate.now().atStartOfDay())
                .build();

        blockFriendRepository.save(block);
    }
}
