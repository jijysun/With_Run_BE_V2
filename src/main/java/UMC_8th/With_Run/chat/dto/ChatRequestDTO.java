package UMC_8th.With_Run.chat.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

public class ChatRequestDTO {

    @Builder @Getter @NoArgsConstructor @AllArgsConstructor
    public static class ShareReqDTO {
        private Boolean isChat;
        private Long userId;
        private Long targetUserId;
        private Long chatId;
        private Long courseId;
    }

    @Builder @Getter @NoArgsConstructor @AllArgsConstructor
    public static class InviteUserReqDTO {
        private String username; // 초대한 사용자 이름
        private List<InviteDTO> inviteUserList;
    }

    @Builder @Getter @NoArgsConstructor @AllArgsConstructor
    public static class InviteDTO{
        private Long userId;
        private String name;
    }


    @Builder @Getter @NoArgsConstructor @AllArgsConstructor
    public static class ChattingReqDTO {
        private Long userId;
        private String message;
        private Boolean isCourse;
    }
}