package UMC_8th.With_Run.chat.entity;


import UMC_8th.With_Run.chat.entity.mapping.UserChat;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Chat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer participants; // 참여자 수 입니다.

    @Column(columnDefinition = "TEXT")
    private String lastReceivedMsg; // Redis!

    @CreatedDate
    @Column(nullable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "chat", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserChat> userChatList = new ArrayList<>();

    @OneToMany(mappedBy = "chat", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Message> messageList = new ArrayList<>();

    public void updateParticipants (int participants){
        this.participants = participants;
    }

    public void addUserChat (UserChat userChat){
        this.getUserChatList().add(userChat);
    }

    public void updateLastReceivedMsg(String lastReceivedMsg){ // Not to @Setter! & Redis!
        this.lastReceivedMsg = lastReceivedMsg;
    }
}


