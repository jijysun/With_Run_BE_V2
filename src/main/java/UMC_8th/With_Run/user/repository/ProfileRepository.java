package UMC_8th.With_Run.user.repository;

import UMC_8th.With_Run.user.entity.Profile;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import UMC_8th.With_Run.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProfileRepository extends JpaRepository<Profile, Long> {
    Optional<Profile> findByUserId(Long userId);

    List<Profile> findAllByUserInOrderByUser_Id(Collection<User> users);

    @Query("select p From Profile p JOIN p.user where p.user in :userList")
    List<Profile> findAllByUserIn(@Param("userList") List<User> userList);

    @Query("select p From Profile p where p.user.id in :userIdList")
    List<Profile> findAllByUser_IdIn(List<Long> userIdList);


}
