package UMC_8th.With_Run.user.entity;

import UMC_8th.With_Run.chat.entity.Message;
import UMC_8th.With_Run.chat.entity.mapping.UserChat;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.springframework.data.annotation.LastModifiedDate;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "login_id", nullable = false, length = 64)
    private String loginId;

    @Column(nullable = false, length = 50)
    private String email;

    //@CreateDate
    private LocalDate createdAt;

    @LastModifiedDate
    private LocalDate updatedAt;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Profile profile; // ✅ Profile과 연관관계

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User follower;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_user_id")
    private User followee;

    @Column(name = "deleted_at")
    private LocalDate deletedAt;

    public boolean isDeleted() {
        return this.deletedAt != null;
    }

    public void delete() {
        this.deletedAt = LocalDate.now();
    }

    @Column(nullable = false)
    @ColumnDefault("true")
    private boolean noticeEnabled = true;

    public void updateNoticeEnabled(boolean noticeEnabled) {
        this.noticeEnabled = noticeEnabled;
    }

    @OneToMany (mappedBy = "user", cascade = CascadeType.ALL)
    private List<UserChat> userChatList = new ArrayList<>();

    @OneToMany (mappedBy = "user", cascade = CascadeType.ALL)
    private List<Message> messageList = new ArrayList<>();

}
