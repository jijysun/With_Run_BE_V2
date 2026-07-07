package UMC_8th.With_Run.friend.repository;

import UMC_8th.With_Run.user.entity.Block;
import UMC_8th.With_Run.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BlockFriendRepository extends JpaRepository<Block, Long> {
    boolean existsByUserIdAndBlockTargetUserId(Long userId, Long targetUserId);

    // 내가 차단한 유저
    List<Block> findByUserIdAndDeletedAtIsNull(Long userId);

    // 나를 차단한 유저
    List<Block> findByBlockTargetUserIdAndDeletedAtIsNull(Long userId);
}
