package UMC_8th.With_Run.friend.repository;

import UMC_8th.With_Run.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FriendsRepository extends JpaRepository<User, Long>, FriendsRepositoryCustom {
    @Query("SELECT f.targetUser.id FROM Follow f WHERE f.user.id = :userId")
    List<Long> findTargetUserIds(@Param("userId") Long userId);
}

