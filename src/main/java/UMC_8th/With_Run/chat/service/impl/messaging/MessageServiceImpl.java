package UMC_8th.With_Run.chat.service.impl.messaging;

import UMC_8th.With_Run.chat.converter.ChatConverter;
import UMC_8th.With_Run.chat.converter.MessageConverter;
import UMC_8th.With_Run.chat.converter.UserChatConverter;
import UMC_8th.With_Run.chat.dto.ChatRequestDTO;
import UMC_8th.With_Run.chat.dto.GPTDTO;
import UMC_8th.With_Run.chat.entity.Chat;
import UMC_8th.With_Run.chat.entity.Message;
import UMC_8th.With_Run.chat.entity.mapping.UserChat;
import UMC_8th.With_Run.chat.repository.ChatRepository;
import UMC_8th.With_Run.chat.repository.MessageRepository;
import UMC_8th.With_Run.chat.repository.UserChatRepository;
import UMC_8th.With_Run.chat.service.MessageService;
import UMC_8th.With_Run.common.apiResponse.status.ErrorCode;
import UMC_8th.With_Run.common.exception.handler.ChatHandler;
import UMC_8th.With_Run.common.exception.handler.CourseHandler;
import UMC_8th.With_Run.common.exception.handler.UserHandler;
import UMC_8th.With_Run.common.redis.dto.PayloadDTO;
import UMC_8th.With_Run.common.redis.pub_sub.RedisPublisher;
import UMC_8th.With_Run.course.entity.Course;
import UMC_8th.With_Run.course.repository.CourseRepository;
import UMC_8th.With_Run.user.entity.User;
import UMC_8th.With_Run.user.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private final UserRepository userRepository;
    private final UserChatRepository userChatRepository;
    private final CourseRepository courseRepository;
    private final ChatRepository chatRepository;
    private final MessageRepository messageRepository;
    private final RedisPublisher redisPublisher;

    @Override
    public void chatting(Long chatId, ChatRequestDTO.ChattingReqDTO reqDTO) {
        User user = userRepository.findByIdWithProfile(reqDTO.getUserId()).orElseThrow(() -> new UserHandler(ErrorCode.WRONG_USER));
        Chat chat = chatRepository.findById(chatId).orElseThrow(() -> new ChatHandler(ErrorCode.EMPTY_CHAT_LIST));
        Message msg = MessageConverter.toMessage(user, chat, reqDTO);

        // 채팅방에 참여하고 있지 않은 사용자의 안읽은 메세지 수 증가
        List<UserChat> userChatList = userChatRepository.findAllByChat_IdAndIsChattingFalse(chatId);
        userChatList.forEach(UserChat::updateUnReadMsg);

        // 메세지 저장
        messageRepository.save(msg);

        // redis 처리 전용 dto 변환,
        PayloadDTO<Object> payloadDTO = PayloadDTO.builder()
                .type("chat")
                .payload(MessageConverter.toBroadCastMsgDTO(user.getId(), chatId, user.getProfile(), msg))
                .build();

        redisPublisher.publishMsg("redis.chat.msg." + chatId, payloadDTO);
    }

    @Override
    @Transactional
    public void shareCourse(ChatRequestDTO.ShareReqDTO reqDTO) {
        /// 여려 명 공유 시 채팅방 공유 로직, 카카오톡 공유 화면 참고!

        User user = userRepository.findById(reqDTO.getUserId()).orElseThrow(() -> new UserHandler(ErrorCode.WRONG_USER));
        Course course = courseRepository.findById(reqDTO.getCourseId()).orElseThrow(() -> new CourseHandler(ErrorCode.WRONG_COURSE)); // 에러 코드 바꾸기

        if (reqDTO.getIsChat()) { // 채팅방 공유 시 채팅방 ID 이용
            Chat chat = chatRepository.findById(reqDTO.getChatId()).orElseThrow(() -> new ChatHandler(ErrorCode.WRONG_CHAT));

            // join fetch 가능하지 않을까?
            UserChat userChat = userChatRepository.findByUser_IdAndChat_Id(reqDTO.getUserId(), reqDTO.getChatId()).orElseThrow(() -> new ChatHandler(ErrorCode.WRONG_CHAT));
            userChat.setToChatting();

            messageRepository.save(MessageConverter.toShareMessage(user, chat, course));

            PayloadDTO<Object> payloadDTO = PayloadDTO.builder()
                    .type("share")
                    .payload(MessageConverter.toBroadCastCourseDTO(user.getId(), chat.getId(), course))
                    .build();

            // 메세지 BroadCast
            redisPublisher.publishMsg("redis.chat.share." + reqDTO.getChatId(), payloadDTO);
        } else { // 친구를 통한 공유, 채팅이 없는 경우 추가
            User targetUser = userRepository.findById(reqDTO.getTargetUserId()).orElseThrow(() -> new UserHandler(ErrorCode.WRONG_USER));
            Chat privateChat = chatRepository.findPrivateChat(user.getId(), targetUser.getId());
            if (privateChat == null) {
                log.info("'shareCourse'/toFriend - privateChat is Null!");
                privateChat = ChatConverter.toNewChatConverter();

                List<UserChat> ucList = new ArrayList<>();
                UserChat newUserChat = UserChatConverter.toNewUserChat(user, targetUser, null, privateChat);
                newUserChat.setToChatting();

                ucList.add(newUserChat);
                ucList.add(UserChatConverter.toNewUserChat(targetUser, user, null, privateChat));

                Chat savedChat = chatRepository.save(privateChat);
                userChatRepository.saveAll(ucList);

                // Save And Broadcast
                messageRepository.save(MessageConverter.toShareMessage(user, savedChat, course));

                PayloadDTO<Object> payloadDTO = PayloadDTO.builder()
                        .type("share")
                        .payload(MessageConverter.toBroadCastCourseDTO(user.getId(), savedChat.getId(), course))
                        .build();

                // 메세지 BroadCast
                redisPublisher.publishMsg("redis.chat.share." + reqDTO.getChatId(), payloadDTO);
            } else { // 친구 공유, 채팅이 존재하는 경우
                log.info("'shareCourse'/toFriend - privateChat is Not Null! id = {}", privateChat.getId());

                UserChat userChat = userChatRepository.findByUser_IdAndChat_Id(reqDTO.getUserId(), reqDTO.getChatId()).orElseThrow(() -> new ChatHandler(ErrorCode.WRONG_CHAT));
                userChat.setToChatting();

                // Save And Broadcast
                messageRepository.save(MessageConverter.toShareMessage(user, privateChat, course));

                PayloadDTO<Object> payloadDTO = PayloadDTO.builder()
                        .type("share")
                        .payload(MessageConverter.toBroadCastCourseDTO(user.getId(), privateChat.getId(), course))
                        .build();

                // 메세지 BroadCast
                redisPublisher.publishMsg("redis.chat.share." + reqDTO.getChatId(), payloadDTO);
            }
        }


    }
}
