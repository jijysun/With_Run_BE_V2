package UMC_8th.With_Run.chat.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;



public class ChatResponseDTO {

    @Getter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class CreateChatDTO {
        private Long chatId;
        private List<ChatResponseDTO.BroadcastMsgDTO> messageList;
    }

    public interface GetChatListSQLDTO {
        Long getChatId();
        String getChatName();
        Integer getUnReadMsg();
        String getLastReceivedMsg();
        Integer getParticipants();
        String getUsernames();
        String getProfileImages();
    }

    @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor // setter...
    public static class GetChatListDTO {
        private Long chatId;
        private String chatName;
        private Integer unReadMsgCount;
        private String lastReceivedMsg;
        private Integer participants;
        private List<String> usernameList;
        private List<String> userProfileList;
    }


    @Getter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class GetInviteUserDTO {
        private Long userId;

        private String name;

        private String profileImage;
    }

    @Getter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class RenameChatDTO{
        private Long chatId;
        private String chatName;
    }

    @Getter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class BroadcastMsgDTO {
        private Long userId;

        private Long chatId;

        private String userName;

        private String userProfileImage;

        private Long messageId;

        private String msg;

        private boolean isCourse;

        private LocalDateTime createdAt;
    }

    @Getter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class BroadcastCourseDTO {
        private Long userId;

        private Long chatId;

        private String msg;

        private boolean isCourse;

        private boolean isSystem;

        private Long courseId;

        private String courseImage;

        private String keyword;

        private LocalDateTime createdAt;
    }
}
