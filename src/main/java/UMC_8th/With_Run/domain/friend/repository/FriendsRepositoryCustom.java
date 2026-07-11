package UMC_8th.With_Run.domain.friend.repository;

import UMC_8th.With_Run.domain.user.entity.User;

import java.util.List;

public interface FriendsRepositoryCustom {
    List<User> findUsersByRegion(Long provinceId, Long cityId, Long townId, Long excludeUserId);
}
