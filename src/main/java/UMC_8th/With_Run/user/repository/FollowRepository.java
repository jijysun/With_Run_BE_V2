package UMC_8th.With_Run.user.repository;

import UMC_8th.With_Run.user.entity.Follow;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FollowRepository extends JpaRepository<Follow, Long> {
    List<Follow> findAllByUserId(Long userId);

    List<Follow> findAllByTargetUserId(Long targetUserId);

    Optional<Follow> findByUserIdAndTargetUserId(Long userId, Long targetUserId);
    // Eager Loading!
    @Query("select f From Follow f JOIN FETCH f.targetUser where f.user.id = :userId order by f.targetUser.id")
    List<Follow> findAllByUserIdWithTargetUser (@Param("userId") Long userId);

    void deleteByUserIdAndTargetUserId(Long followerId, Long followingId);

}

