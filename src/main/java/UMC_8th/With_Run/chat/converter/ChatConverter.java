package UMC_8th.With_Run.chat.converter;

import UMC_8th.With_Run.chat.dto.ChatResponseDTO;
import UMC_8th.With_Run.chat.entity.Chat;
import UMC_8th.With_Run.user.entity.Profile;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class ChatConverter {

    public static ChatResponseDTO.GetChatListDTO toGetChatListDTO(ChatResponseDTO.GetChatListSQLDTO getChatListSQLDTO, List<String> usernameList, List<String> profileList) {
        return ChatResponseDTO.GetChatListDTO.builder()
                .chatId(getChatListSQLDTO.getChatId())
                .chatName(getChatListSQLDTO.getChatName())
                .unReadMsgCount(getChatListSQLDTO.getUnReadMsg())
                .lastReceivedMsg(getChatListSQLDTO.getLastReceivedMsg())
                .usernameList(usernameList)
                .userProfileList(profileList)
                .participants(getChatListSQLDTO.getParticipants())
                .build();

    }

    public static ChatResponseDTO.GetChatListDTO toGetChatListDTOWithRedis(ChatResponseDTO.GetChatListSQLDTO getChatListSQLDTO, List<String> usernameList, List<String> profileList, Integer unReadMsg, String lastReceivedMsg) {
        return ChatResponseDTO.GetChatListDTO.builder()
                .chatId(getChatListSQLDTO.getChatId())
                .chatName(getChatListSQLDTO.getChatName())
                .unReadMsgCount(unReadMsg)
                .lastReceivedMsg(lastReceivedMsg)
                .usernameList(usernameList)
                .userProfileList(profileList)
                .participants(getChatListSQLDTO.getParticipants())
                .build();

    }

    public static ChatResponseDTO.CreateChatDTO toCreateChatDTO (Long chatId, List<ChatResponseDTO.BroadcastMsgDTO> chatHistory) {
        return ChatResponseDTO.CreateChatDTO.builder()
                .chatId(chatId)
                .messageList(chatHistory)
                .build();
    }

    public static Chat toNewChatConverter() {
        return Chat.builder()
                .userChatList(new ArrayList<>())
                .messageList(new ArrayList<>())
                .participants(2)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    public static List<ChatResponseDTO.GetInviteUserDTO> toGetInviteUserDTO(List<Long> userList, List<Profile> profileList) {
        List<ChatResponseDTO.GetInviteUserDTO> dtoList = new ArrayList<>();

        for (int i = 0; i < userList.size(); i++) {
            dtoList.add(ChatResponseDTO.GetInviteUserDTO.builder()
                    .userId(userList.get(i))
                    .name(profileList.get(i).getName())
                    .profileImage(profileList.get(i).getProfileImage())
                    .build());
        }

        return dtoList;
    }
}
