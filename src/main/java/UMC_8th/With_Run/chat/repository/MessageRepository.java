package UMC_8th.With_Run.chat.repository;

import UMC_8th.With_Run.chat.entity.Chat;
import UMC_8th.With_Run.chat.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {
    @Query("Select m From Message m JOIN fetch m.user u JOIN fetch u.profile where m.chat.id = :chatId") // Join Fetch!
    List<Message> findByChat_Id(@Param("chatId") Long chatId);

    Page<Message> findByChat_IdAndIdLessThanOrderById(Long chatId, Integer idIsLessThan, Pageable pageable);

    Page<Message> findByChat_IdAndIdLessThan(Long chatId, Integer idIsLessThan, Pageable pageable);


    @Query("SELECT m From Message m join fetch m.user u WHERE m.chat.id = :chatId AND m.createdAt >= :joinTime ORDER BY m.id DESC ")
    List<Message> getLastestMessagesByChatId (@Param("chatId") Long chatId, @Param("joinTime") LocalDateTime joinTime, Pageable pageable);

    @Query ("SELECT m From Message m JOIN FETCH m.user u WHERE m.chat.id = :chatId AND m.createdAt >= :joinTime AND m.id < :cursorId ORDER BY m.id DESC")
    List<Message> getPreviousMessagesByChatId (@Param("chatId") Long chatId, @Param("joinTime") LocalDateTime joinTime, @Param("cursorId") Long cursorId,  Pageable pageable);
}
