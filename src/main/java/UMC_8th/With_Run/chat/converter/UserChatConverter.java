package UMC_8th.With_Run.chat.converter;

import UMC_8th.With_Run.chat.entity.Chat;
import UMC_8th.With_Run.chat.entity.mapping.UserChat;
import UMC_8th.With_Run.user.entity.User;

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
