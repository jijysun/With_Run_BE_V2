package UMC_8th.With_Run.domain.chat.converter;

import UMC_8th.With_Run.domain.chat.entity.Chat;
import UMC_8th.With_Run.domain.chat.entity.mapping.UserChat;
import UMC_8th.With_Run.domain.user.entity.User;

import java.time.LocalDateTime;

public class UserChatConverter {

    public static UserChat toNewUserChat(User user, User targetUser, String chatName, Chat chat) {
        return UserChat.builder()
                .user(user)
                .chat(chat)
                .chatName(targetUser != null ? targetUser.getProfile().getName() : chatName)
                .unReadMsg(0)
                .isChatting(false)
                .isDefaultChatName(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
