package UMC_8th.With_Run.user.repository;

import UMC_8th.With_Run.user.entity.User;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByEmailAndLoginId(String email, String loginId);

    @Query("Select u From User u join fetch u.profile where u.id = :id")
    Optional<User> findByIdWithProfile(@Param ("id")Long userId);

    boolean existsByIdAndNoticeEnabledTrue(Long userId);


    @Query("Select u.id From User u join UserChat uc on u.id=uc.user.id where uc.chat.id = :chatId")
    List<Long> findAllUserId(@Param("chatId") Long chatId);
}
