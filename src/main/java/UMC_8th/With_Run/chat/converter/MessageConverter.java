package UMC_8th.With_Run.chat.converter;

import UMC_8th.With_Run.chat.dto.ChatRequestDTO;
import UMC_8th.With_Run.chat.dto.ChatResponseDTO;
import UMC_8th.With_Run.chat.entity.Chat;
import UMC_8th.With_Run.chat.entity.Message;
import UMC_8th.With_Run.course.entity.Course;
import UMC_8th.With_Run.user.entity.Profile;
import UMC_8th.With_Run.user.entity.User;

import java.time.LocalDateTime;
import java.util.List;

public class MessageConverter {

    public static Message toMessage(User user, Chat chat, ChatRequestDTO.ChattingReqDTO dto) {
        return Message.builder()
                .user(user)
                .chat(chat)
                .isCourse(false)
                .msg(dto.getMessage())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    public static List<ChatResponseDTO.BroadcastMsgDTO> toChatHistoryDTO(List<Message> messageList, Long chatId) {
        return messageList.stream()
                .map(message -> ChatResponseDTO.BroadcastMsgDTO.builder()
                        .userId(message.getUser().getId())
                        .userName(message.getUser().getProfile().getName())
                        .userProfileImage(message.getUser().getProfile().getProfileImage())
                        .msg(message.getMsg())
                        .isCourse(false)
                        .chatId(chatId)
                        .messageId(message.getId())
                        .createdAt(message.getCreatedAt())
                        .build())
                .toList();
    }

    public static Message toInviteMessage(User user,Chat chat, String msg) {
        return Message.builder()
                .user(user)
                .chat(chat)
                .isCourse(false)
                .msg(msg)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    public static Message toShareMessage(User user, Chat chat, Course course) {
        return Message.builder()
                .user(user)
                .chat(chat)
                .isCourse(true)
                .msg("산책 코스를 공유하였습니다")
                .courseId(course.getId())
                .courseImage(course.getCourseImage())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    public static Message toFirstChatMessage (User user, Chat chat){
        return Message.builder()
                .user(user)
                .chat(chat)
                .isCourse(false)
                .msg("상대방과 나누는 첫 대화입니다!")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

    }

    public static ChatResponseDTO.BroadcastMsgDTO toBroadCastMsgDTO(Long userId, Long chatId, Profile profile, Message message) {
        return ChatResponseDTO.BroadcastMsgDTO.builder()
                .userId(userId)
                .chatId(chatId)
                .userName(profile.getName())
                .userProfileImage(profile.getProfileImage())
                .msg(message.getMsg())
                .isCourse(false)
                .createdAt(message.getCreatedAt())
                .build();
    }

    public static ChatResponseDTO.BroadcastCourseDTO toBroadCastCourseDTO(Long userId, Long chatId, Course course) {
        return ChatResponseDTO.BroadcastCourseDTO.builder()
                .userId(userId)
                .chatId(chatId)
                .msg("산책 코스를 공유하였습니다")
                .isCourse(true)
                .isSystem(true)
                .courseId(course.getId())
                .courseImage(course.getCourseImage())
                .keyword(course.getKeyWord())
                .createdAt(LocalDateTime.now())
                .build();
    }
}
