package UMC_8th.With_Run.friend.repository;

import UMC_8th.With_Run.user.entity.Follow;
import UMC_8th.With_Run.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FollowFriendRepository extends JpaRepository<Follow, Long> {
    boolean existsByUserAndTargetUser(User user, User targetUser);
}

