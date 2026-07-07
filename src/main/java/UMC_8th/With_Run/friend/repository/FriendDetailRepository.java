package UMC_8th.With_Run.friend.repository;

import UMC_8th.With_Run.user.entity.Profile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FriendDetailRepository extends JpaRepository<Profile, Long> {
    Optional<Profile> findByUserIdAndDeletedAtIsNull(Long userId);
}