package UMC_8th.With_Run.domain.friend.repository;

import UMC_8th.With_Run.domain.user.entity.Follow;
import UMC_8th.With_Run.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FollowFriendRepository extends JpaRepository<Follow, Long> {
    boolean existsByUserAndTargetUser(User user, User targetUser);
}

