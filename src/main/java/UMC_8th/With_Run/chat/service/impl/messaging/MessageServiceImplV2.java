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
@Service
@RequiredArgsConstructor
public class MessageServiceImplV2 implements MessageService {

    private final UserRepository userRepository;
    private final UserChatRepository userChatRepository;
    private final CourseRepository courseRepository;
    private final ChatRepository chatRepository;
    private final MessageRepository messageRepository;
    private final RedisPublisher redisPublisher;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${chatgpt.api.key}")
    private String API_KEY;

    @Value("${chatgpt.api.uri}")
    private String API_URI;

    @Override
    @Transactional
    public void chatting(Long chatId, ChatRequestDTO.ChattingReqDTO reqDTO) {
        User user = userRepository.findByIdWithProfile(reqDTO.getUserId()).orElseThrow(() -> new UserHandler(ErrorCode.WRONG_USER));
        Chat chat = chatRepository.findById(chatId).orElseThrow(() -> new ChatHandler(ErrorCode.EMPTY_CHAT_LIST));
        Message msg = MessageConverter.toMessage(user, chat, reqDTO);
        List<Message> messageList = new ArrayList<>();
        messageList.add(msg);

        /* 채팅 메세지 파싱 사항
         * 1. 같이 산책 코스 약속을 잡은 경우, isUpToMeet -> AI
         * 2. 개인 정보를 보낸 경우, isPrivacy -> 자체 파싱
         * 3. 이후 추가 약속 고려?, isMeetAgain -> AI
         * 4. 펫코노미 고려, isPetConomy -> AI
         * */

        GPTDTO.GPTAnswerDTO gptAnswerDTO = new GPTDTO.GPTAnswerDTO();
        boolean isPrivacy = false;

        List<String> privacy = List.of(
                "\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\\b", // Email
                "\\b010[- ]?\\d{4}[- ]?\\d{4}\\b", // Phone
                "(?:[가-힣]+(시|도)\\s*)?[가-힣]+(시|군|구)\\s*[가-힣0-9]+(읍|면|동|리)\\s*\\d+(?:-\\d+)?번?지?" // Address
        );

        if (privacy.stream()
                .anyMatch(pattern -> reqDTO.getMessage().matches(pattern))) {
            isPrivacy = true;
        } else {
//            gptAnswerDTO = requestToAI(reqDTO);
        }

        List<Long> userChatList = userChatRepository.findAllByChat_Id(chatId);

        userChatList.forEach(userChat -> {
            String key = "user:" + userChat + ":" + chatId;
            String isChatting = redisTemplate.opsForHash().get(key, "isChatting").toString();
            if (isChatting.equals("false")) {
                redisTemplate.opsForHash().increment(key, "unReadMsg", 1);
            }
        });

        /*Message aiMessage = MessageConverter.toInviteMessage(user, chat, gptAnswerDTO.getMessage());
        if (! gptAnswerDTO.getAnswer().isEmpty()|| !gptAnswerDTO.getAnswer().equals("nothing") || !gptAnswerDTO.getMessage().isEmpty()) {
            messageList.add(aiMessage);
        }*/

        // 메세지 저장
        messageRepository.saveAll(messageList);
//        chat.updateLastReceivedMsg(reqDTO.getMessage());
        redisTemplate.opsForHash().put("chat:" + chatId, "lastReceivedMsg", reqDTO.getMessage());

        PayloadDTO<Object> payloadDTO = PayloadDTO.builder() // redis 처리 전용 dto 변환,
                .type("chat")
                .payload(MessageConverter.toBroadCastMsgDTO(user.getId(), chatId, user.getProfile(), msg))
                .build();

        redisPublisher.publishMsg("redis.chat.msg." + chatId, payloadDTO);

        if (isPrivacy) {
            Message privacyMsg = MessageConverter.toInviteMessage(user, chat, "\uD83D\uDD12 개인정보가 보이는 정보가 메세지로 보내졌어요, 개인정보 유출에 주의해주세요!");
            redisPublisher.publishMsg("redis.chat.msg." + chatId, privacyMsg);
        } /*else if (!gptAnswerDTO.getAnswer().equals("nothing")) {
            PayloadDTO<Object> payloadMeetInfoDTO = PayloadDTO.builder() // redis 처리 전용 dto 변환,
                    .type("chat")
                    .payload(MessageConverter.toBroadCastMsgDTO(user.getId(), chatId, user.getProfile(), aiMessage))
                    .build();
            redisPublisher.publishMsg("redis.chat.msg." + chatId, payloadMeetInfoDTO);
        }*/
    }

    private GPTDTO.GPTAnswerDTO requestToAI(ChatRequestDTO.ChattingReqDTO reqDTO) {
        GPTDTO.GPTAnswerDTO gptAnswerDTO;
        GPTDTO.GPTResponseDTO gptResponseDTO;
        // request to AI!
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(API_KEY);

        GPTDTO.GPTRequestDTO requestDTO = GPTDTO.GPTRequestDTO.builder()
                .model("gpt-3.5-turbo")
                .messages(List.of(GPTDTO.GPTMessage.builder()
                                .role("system")
                                .content("""
                                        채팅 메세지를 분석하는 AI 로 프롬프트 튜닝 중.
                                        결과는 반드시 JSON 형식, {"answer" : "", "message":""}
                                        조건
                                        - message는 해당 분석 후 '약속을 잡으셨군요!' 과 함께 50 글자 내로 산책할 때 좋은 정보 추천할 것, 장소 언급은 제외.
                                        - 약속 잡은 문자인 경우 -> answer : "isUpToMeet"
                                        - 위 조건에 해당 되지 않음 -> answer : "nothing"
                                        - 다른 텍스트는 포함하지 말 것. 추가적인 대화로 이어지는 답변도 금지.
                                        """)
                                .build(),
                        GPTDTO.GPTMessage.builder()
                                .role("user")
                                .content(reqDTO.getMessage())
                                .build()))
//                    .max_completion_tokens(2000)
                .build();

        log.info("request start");

        HttpEntity<GPTDTO.GPTRequestDTO> request = new HttpEntity<>(requestDTO, headers);
        ResponseEntity<String> response = restTemplate.exchange(API_URI, HttpMethod.POST, request, String.class);
        log.info("request end");

        log.info("body: {}", response.getBody()); // GPT Log!
        try {
            gptResponseDTO = objectMapper.readValue(response.getBody(), GPTDTO.GPTResponseDTO.class);
            gptAnswerDTO = objectMapper.readValue(gptResponseDTO.getChoices().get(0).getMessage().getContent(), GPTDTO.GPTAnswerDTO.class);
        } catch (JsonProcessingException e) {
            throw new ChatHandler(ErrorCode.CANT_PARSING_AI_MAG);
        }
        return gptAnswerDTO;
    }

    @Override
    @Transactional
    public void shareCourse(ChatRequestDTO.ShareReqDTO reqDTO) { /// 여려 명 공유 시 채팅방 공유 로직, 카카오톡 공유 화면 참고!

        User user = userRepository.findById(reqDTO.getUserId()).orElseThrow(() -> new UserHandler(ErrorCode.WRONG_USER));
        Course course = courseRepository.findById(reqDTO.getCourseId()).orElseThrow(() -> new CourseHandler(ErrorCode.WRONG_COURSE)); // 에러 코드 바꾸기

        if (reqDTO.getIsChat()) { // 채팅방 공유 시 채팅방 ID 이용
            Chat chat = chatRepository.findById(reqDTO.getChatId()).orElseThrow(() -> new ChatHandler(ErrorCode.WRONG_CHAT));
            List<Long> userChatList = userChatRepository.findAllByChat_Id(chat.getId());

            userChatList.forEach(userChat -> {
                String key = "user:" + userChat + ":" + chat.getId();
                String isChatting = redisTemplate.opsForHash().get(key, "isChatting").toString();
                if (isChatting.equals("false")) {
                    redisTemplate.opsForHash().increment(key, "unReadMsg", 1);
                }
            });

            String key = "user:" + user.getId() + ":" + chat.getId();
            redisTemplate.opsForHash().put(key, "isChatting", "true");

            messageRepository.save(MessageConverter.toShareMessage(user, chat, course));

            PayloadDTO<Object> payloadDTO = PayloadDTO.builder()
                    .type("share")
                    .payload(MessageConverter.toBroadCastCourseDTO(user.getId(), chat.getId(), course))
                    .build();

            // 메세지 BroadCast
//            chat.updateLastReceivedMsg("산책 코스를 공유하였습니다");
            redisTemplate.opsForHash().put("chat:" + chat.getId(), "lastReceivedMsg", "산책 코스를 공유하였습니다");
            redisPublisher.publishMsg("redis.chat.share." + reqDTO.getChatId(), payloadDTO);
        } else { // 친구를 통한 공유, 채팅이 없는 경우 추가
            User targetUser = userRepository.findById(reqDTO.getTargetUserId()).orElseThrow(() -> new UserHandler(ErrorCode.WRONG_USER));
            Chat privateChat = chatRepository.findPrivateChat(user.getId(), targetUser.getId());
            if (privateChat == null) {
                log.info("'shareCourse'/toFriend - privateChat is Null!");
                privateChat = ChatConverter.toNewChatConverter();

                List<UserChat> ucList = new ArrayList<>();
                UserChat newUserChat = UserChatConverter.toNewUserChat(user, targetUser, null, privateChat);

                ucList.add(newUserChat);
                ucList.add(UserChatConverter.toNewUserChat(targetUser, user, null, privateChat));

                Chat savedChat = chatRepository.save(privateChat);
                redisTemplate.opsForHash().put("user:" + user.getId() + ":" + savedChat.getId(), "isChatting", "true");
                redisTemplate.opsForHash().put("user:" + user.getId() + ":" + savedChat.getId(), "unReadMsg", "0");
                redisTemplate.opsForHash().put("user:" + targetUser.getId() + ":" + savedChat.getId(), "isChatting", "false");
                redisTemplate.opsForHash().put("user:" + targetUser.getId() + ":" + savedChat.getId(), "unReadMsg", "1");

                userChatRepository.saveAll(ucList);

                // Save And Broadcast
                messageRepository.save(MessageConverter.toShareMessage(user, savedChat, course));
//                savedChat.updateLastReceivedMsg("산책 코스를 공유하였습니다");
                redisTemplate.opsForHash().put("chat:" + savedChat.getId(), "lastReceivedMsg", "산책 코스를 공유하였습니다");
                redisTemplate.opsForHash().increment("user:" + targetUser.getId() + ":" + savedChat.getId() , "unReadMsg", 1);

                PayloadDTO<Object> payloadDTO = PayloadDTO.builder()
                        .type("share")
                        .payload(MessageConverter.toBroadCastCourseDTO(user.getId(), savedChat.getId(), course))
                        .build();

                // 메세지 BroadCast
                redisPublisher.publishMsg("redis.chat.share." + reqDTO.getChatId(), payloadDTO);
            } else { // 친구 공유, 채팅이 존재하는 경우
                Long privateChatId = privateChat.getId();
                log.info("'shareCourse'/toFriend - privateChat is Not Null! id = {}", privateChatId);

                String key = "user:"+user.getId() + ":"+ privateChatId;
                redisTemplate.opsForHash().put(key,  "isChatting", "true");

                // Save And Broadcast
                messageRepository.save(MessageConverter.toShareMessage(user, privateChat, course));

                List<Long> userChatList = userChatRepository.findAllByChat_Id(privateChatId);

                userChatList.forEach(userChat -> {
                    String thisKey = "user:" + userChat + ":" + privateChatId;
                    String isChatting = redisTemplate.opsForHash().get(thisKey, "isChatting").toString();
                    if (isChatting.equals("false")) {
                        redisTemplate.opsForHash().increment(thisKey, "unReadMsg", 1);
                    }
                });

                redisTemplate.opsForHash().put("chat:" + privateChatId, "lastReceivedMsg", "산책 코스를 공유하였습니다");

                PayloadDTO<Object> payloadDTO = PayloadDTO.builder()
                        .type("share")
                        .payload(MessageConverter.toBroadCastCourseDTO(user.getId(), privateChatId, course))
                        .build();

                // 메세지 BroadCast
                redisPublisher.publishMsg("redis.chat.share." + reqDTO.getChatId(), payloadDTO);
            }
        }


    }
}
