package UMC_8th.With_Run.friend.repository;

import UMC_8th.With_Run.user.entity.User;

import java.util.List;

public interface FriendsRepositoryCustom {
    List<User> findUsersByRegion(Long provinceId, Long cityId, Long townId, Long excludeUserId);
}
