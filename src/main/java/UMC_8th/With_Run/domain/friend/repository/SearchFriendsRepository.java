package UMC_8th.With_Run.domain.friend.repository;

import UMC_8th.With_Run.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SearchFriendsRepository extends JpaRepository<User, Long>, SearchFriendsRepositoryCustom{
    List<User> searchFriendsByKeyword(Long provinceId, Long cityId, Long townId, Long userId, String keyword);
}
