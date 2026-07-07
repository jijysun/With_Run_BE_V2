package UMC_8th.With_Run.notice.entity;

import UMC_8th.With_Run.user.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
public class Notice {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long receiverId; // 알림 받는 유저
    private Long actorId;    // 알림 발생시킨 유저
    private Long targetId;   // 관련 게시글 ID 등

    private String message;

    @Enumerated(EnumType.STRING)
    private NoticeType type;

    private LocalDateTime createdAt;

    public Notice(Long receiverId, Long actorId, Long targetId, String message, NoticeType type) {
        this.receiverId = receiverId;
        this.actorId = actorId;
        this.targetId = targetId;
        this.message = message;
        this.type = type;
        this.createdAt = LocalDateTime.now();
    }

    public Notice(User receiver, User actor, Long targetId, NoticeType type) {
        this.receiverId = receiver.getId();
        this.actorId = actor.getId();
        this.targetId = targetId;
        this.type = type;
        this.createdAt = LocalDateTime.now();
        this.message = message;
    }
}
