package UMC_8th.With_Run.chat.repository;

import UMC_8th.With_Run.chat.entity.Chat;
import UMC_8th.With_Run.chat.entity.mapping.UserChat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;


public interface ChatRepository extends JpaRepository<Chat, Long>{


    List<Chat> findAllByUserChatListIn(List<UserChat> userChatList);

    @Query ("select c from Chat c JOIN UserChat uc on c.id = uc.chat.id " +
            "where uc.user.id in (:user1Id, :user2Id) And c.participants = 2 " +
            "group by c.id HAVING count (DISTINCT uc.user.id) = 2")
    Chat findPrivateChat (Long  user1Id, Long user2Id);
}
