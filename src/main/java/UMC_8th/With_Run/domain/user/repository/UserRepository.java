package UMC_8th.With_Run.domain.user.repository;

import UMC_8th.With_Run.domain.user.entity.User;

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

    // STOMP 인증(StompChannelInterceptor)이 심어준 Principal의 email로 실제 User를 조회할 때 사용.
    // reqDTO의 클라이언트 제공 userId 대신 이 조회 결과만 신원 판단에 쓴다.
    @Query("Select u From User u join fetch u.profile where u.email = :email")
    Optional<User> findByEmailWithProfile(@Param("email") String email);

    boolean existsByIdAndNoticeEnabledTrue(Long userId);


    @Query("Select u.id From User u join UserChat uc on u.id=uc.user.id where uc.chat.id = :chatId")
    List<Long> findAllUserId(@Param("chatId") Long chatId);

    long countByLoginIdStartingWith(String prefix);

    @Query("SELECT u FROM User u WHERE u.loginId LIKE CONCAT(:prefix, '%') AND u.userChatList IS EMPTY ORDER BY u.id")
    List<User> findUnassignedByLoginIdPrefix(@Param("prefix") String prefix);
}
