package UMC_8th.With_Run.domain.chat.service.impl.messaging;

import UMC_8th.With_Run.domain.chat.converter.ChatConverter;
import UMC_8th.With_Run.domain.chat.converter.MessageConverter;
import UMC_8th.With_Run.domain.chat.converter.UserChatConverter;
import UMC_8th.With_Run.domain.chat.dto.ChatRequestDTO;
import UMC_8th.With_Run.domain.chat.dto.GPTDTO;
import UMC_8th.With_Run.domain.chat.entity.Chat;
import UMC_8th.With_Run.domain.chat.entity.Message;
import UMC_8th.With_Run.domain.chat.entity.mapping.UserChat;
import UMC_8th.With_Run.domain.chat.repository.ChatRepository;
import UMC_8th.With_Run.domain.chat.repository.MessageRepository;
import UMC_8th.With_Run.domain.chat.repository.UserChatRepository;
import UMC_8th.With_Run.domain.chat.service.MessageService;
import UMC_8th.With_Run.global.apiResponse.status.ErrorCode;
import UMC_8th.With_Run.global.exception.handler.ChatHandler;
import UMC_8th.With_Run.global.exception.handler.CourseHandler;
import UMC_8th.With_Run.global.exception.handler.UserHandler;
import UMC_8th.With_Run.global.redis.dto.PayloadDTO;
import UMC_8th.With_Run.global.redis.pub_sub.RedisPublisher;
import UMC_8th.With_Run.global.scheduler.RedisSyncScheduler;
import UMC_8th.With_Run.domain.course.entity.Course;
import UMC_8th.With_Run.domain.course.repository.CourseRepository;
import UMC_8th.With_Run.domain.user.entity.Profile;
import UMC_8th.With_Run.domain.user.entity.User;
import UMC_8th.With_Run.domain.user.repository.UserRepository;
import UMC_8th.With_Run.domain.user.service.UserService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.script.RedisScript;
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
    private final MeterRegistry meterRegistry;
    private final RedisSyncScheduler redisSyncScheduler;
    private final RedisScript<Long> unreadIncrementScript;
    private final UserService userService;

    @Value("${chatgpt.api.key}")
    private String API_KEY;

    @Value("${chatgpt.api.uri}")
    private String API_URI;


    /**
     *
     * 본격 채팅 메서드
     * - 쿼리 발생: JWT 인증, User 조회, Chat 조회, UserChatList 조회, MessageRepository.saveAll() = 약 5번
     * - Redis 접근 발생: 118, 125줄 + stringRedisTemplate.convertAndSend(topic, msg);
     *
     * 추가 예정 기능
     * 1. 같이 산책 코스 약속을 잡은 경우, isUpToMeet -> AI
     * 2. 개인 정보를 보낸 경우, isPrivacy -> 자체 파싱
     * 3. 이후 추가 약속 고려?, isMeetAgain -> AI
     * 4. 펫코노미 고려, isPetConomy -> AI
     *
     * 특이사항
     * - 서버 사이드 순수 처리시간( 수신 ~ Redis publish 완료)만 측정
     * - 클라이언트-서버 왕복(k6 correlation ID)과 구분해서, 병목이 서버 처리 자체인지 네트워크 왕복인 지 확인할 수 있게끔.
     * - chatId 별 태그는 넣지 않음 — 방이 수백 개로 늘면 Prometheus 카디널리티가 폭발!
     *
     * 문제사항
     * - RaceCondition1: 두 사용자 -> 다른 친구 동시 초대 시 정원 초과 가능.
     * - RaceCondition2: 읽지 않은 메시지 카운팅 -> redisTemplate.opsForHash().get(key, "isChatting").toString()이 Null일 경우 NPE 발생
     * - 안읽은 메시지 처리 기능: userChatRepository.findAllByChat_Id(chatId) 만큼 Redis Hash가 발생.
     * - RaceConditions3: ChatService.createChat()에 대한 RC 발생 (정확히는 LostUpdate가 발생하지 않을까?)
     *
     *
     * @param chatId
     * @param reqDTO
     */
    @Override
    @Transactional
    public void chatting(Long chatId, ChatRequestDTO.ChattingReqDTO reqDTO, String authenticatedEmail) {
        // 서버 사이드 순수 처리시간( 수신 ~ Redis publish 완료)만 측정
        // — 클라이언트-서버 왕복(k6 correlation ID)과 구분해서, 병목이 서버 처리 자체인지 네트워크 왕복인 지 확인할 수 있게끔.
        // chatId 별 태그는 넣지 않음 — 방이 수백 개로 늘면 Prometheus 카디널리티가 폭발!
        Timer.Sample sample = Timer.start(meterRegistry);

        // 발신자 조회: reqDTO.getUserId() ->  STOMP CONNECT에서 검증된 authenticatedEmail로만 조회 = userId 위조 가능
        // User/Profile을 Caffeine 캐시(userCache/profileCache, 서로 다른 TTL)로 분리 조회 —
        // Cache Hit = SQL 자체가 안나감, 로그가 나가도 SQL이 발생한 건 아님.
        log.info("SELECT 시도(Cache HIT 시 SQL 생략) -> chatting(): User (email={})", authenticatedEmail);
        User user = userService.getCachedUser(authenticatedEmail);
        log.info("SELECT 시도(Cache HIT 시 SQL 생략) -> chatting(): Profile (userId={})", user.getId());
        Profile profile = userService.getCachedProfile(user.getId());

        log.info("SQL 발생! -> chatting(): SELECT Chat (chatId={})", chatId);
        Chat chat = chatRepository.findById(chatId).orElseThrow(() -> new ChatHandler(ErrorCode.EMPTY_CHAT_LIST));

        /// 메세지 객체 하나만 저장하는데 왜 리스트화 하는 것?
        Message msg = MessageConverter.toMessage(user, chat, reqDTO);
        List<Message> messageList = new ArrayList<>();
        messageList.add(msg);


        // 2. 안읽은 메세지 기능, 메세지에 따른 채팅방에 속한 유저에 대한 unReadMsg increment
        log.info("SQL 발생! -> chatting(): SELECT UserChatList (chatId={})", chatId);
        List<Long> userChatList = userChatRepository.findAllByChat_Id(chatId);

        log.info("Redis PipeLine I/O 발생! -> chatting(): EVAL(unreadIncrement) × {}건, 파이프라인으로 1회 왕복 처리", userChatList.size());
        // SessionCallback: RedisConnection을 직접 다루지 않고, RedisOperations의 타입-세이프 API(execute(script, keys, args)) 사용 가능!
        redisTemplate.executePipelined(new SessionCallback<Object>() { // pipeLined = RTT 개선 부여
            @Override
            public Object execute(RedisOperations operations) {
                userChatList.forEach(userChat -> {
                    String key = "user:" + userChat + ":" + chatId;
                    // .lua = 원자성 부여
                    operations.execute(unreadIncrementScript, List.of(key, RedisSyncScheduler.DIRTY_USER_CHAT_KEY), "1");
                });
                return null;
            }
        });


        // 3. 메세지 저장
        // - messageRepository.saveAll() + redisPublisher.publishMsg = mysql 저장 및 Redis 발행
        log.info("SQL 발생! -> chatting(): INSERT Message (chatId={}, userId={})", chatId, user.getId());
        messageRepository.saveAll(messageList);
//        chat.updateLastReceivedMsg(reqDTO.getMessage());
        String chatKey = "chat:" + chatId;
        log.info("Redis I/O 발생! -> chatting(): HPUT (key={}, field=lastReceivedMsg, value={})", chatKey, reqDTO.getMessage());
        redisTemplate.opsForHash().put(chatKey, "lastReceivedMsg", reqDTO.getMessage());
        redisSyncScheduler.markingDirtyChat(chatKey);

        PayloadDTO<Object> payloadDTO = PayloadDTO.builder() // redis 처리 전용 dto 변환,
                .type("chat")
                // user.getProfile()은 더 이상 쓰면 안 됨 — user가 findByEmail(join fetch 없음)로 조회됨
                // + profile은 지연 로딩 프록시고, 캐시로 재사용되는 detached 상태라 세션이 없어 LazyInitializationException이 남.
                // 대신 별도로 캐시 조회한 profile을 그대로 사용.
//                .payload(MessageConverter.toBroadCastMsgDTO(user.getId(), chatId, user.getProfile(), msg))
                .payload(MessageConverter.toBroadCastMsgDTO(user.getId(), chatId, profile, msg))
                .build();

        String publishTopic = "redis.chat.msg." + chatId;
        log.info("Redis I/O 발생! -> chatting(): PUBLISH (topic={})", publishTopic);
        redisPublisher.publishMsg(publishTopic, payloadDTO);

        Counter.builder("chat_messages_sent_total")
                .description("MySQL 저장 + Redis publish까지 성공적으로 완료된 채팅 메시지 수")
                .register(meterRegistry)
                .increment();
        sample.stop(Timer.builder("chat_message_processing_seconds")
                .description("chatting() 진입부터 Redis publish 완료까지 서버 처리 시간(네트워크 왕복 제외)")
                .register(meterRegistry));
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
        // chatting()과 동일한 계측 패턴 — 앞으로 주 테스트 시나리오에 shareCourse() 호출이 추가될 예정이라 미리 부착.
        Timer.Sample sample = Timer.start(meterRegistry);

        User user = userRepository.findById(reqDTO.getUserId()).orElseThrow(() -> new UserHandler(ErrorCode.WRONG_USER));
        Course course = courseRepository.findById(reqDTO.getCourseId()).orElseThrow(() -> new CourseHandler(ErrorCode.WRONG_COURSE)); // 에러 코드 바꾸기

        if (reqDTO.getIsChat()) { // 채팅방 공유 시 채팅방 ID 이용
            Chat chat = chatRepository.findById(reqDTO.getChatId()).orElseThrow(() -> new ChatHandler(ErrorCode.WRONG_CHAT));
            List<Long> userChatList = userChatRepository.findAllByChat_Id(chat.getId());

            userChatList.forEach(userChat -> {
                String participantKey = "user:" + userChat + ":" + chat.getId();
                String isChatting = redisTemplate.opsForHash().get(participantKey, "isChatting").toString();
                if (isChatting.equals("false")) {
                    redisTemplate.opsForHash().increment(participantKey, "unReadMsg", 1);
                    redisSyncScheduler.markingDirtyUserChat(participantKey);
                }
            });

            String key = "user:" + user.getId() + ":" + chat.getId();
            redisTemplate.opsForHash().put(key, "isChatting", "true");
            redisSyncScheduler.markingDirtyUserChat(key);

            messageRepository.save(MessageConverter.toShareMessage(user, chat, course));

            PayloadDTO<Object> payloadDTO = PayloadDTO.builder()
                    .type("share")
                    .payload(MessageConverter.toBroadCastCourseDTO(user.getId(), chat.getId(), course))
                    .build();

            // 메세지 BroadCast
//            chat.updateLastReceivedMsg("산책 코스를 공유하였습니다");
            String shareChatKey = "chat:" + chat.getId();
            redisTemplate.opsForHash().put(shareChatKey, "lastReceivedMsg", "산책 코스를 공유하였습니다");
            redisSyncScheduler.markingDirtyChat(shareChatKey);
            redisPublisher.publishMsg("redis.chat.share." + reqDTO.getChatId(), payloadDTO);
            Counter.builder("chat_share_sent_total")
                    .description("MySQL 저장 + Redis publish까지 성공적으로 완료된 산책 코스 공유 수")
                    .register(meterRegistry)
                    .increment();
            sample.stop(Timer.builder("chat_share_processing_seconds")
                    .description("shareCourse() 진입부터 Redis publish 완료까지 서버 처리 시간(네트워크 왕복 제외)")
                    .register(meterRegistry));
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
                String shareNewSelfKey = "user:" + user.getId() + ":" + savedChat.getId();
                String shareNewTargetKey = "user:" + targetUser.getId() + ":" + savedChat.getId();
                redisTemplate.opsForHash().put(shareNewSelfKey, "isChatting", "true");
                redisTemplate.opsForHash().put(shareNewSelfKey, "unReadMsg", "0");
                redisTemplate.opsForHash().put(shareNewTargetKey, "isChatting", "false");
                redisTemplate.opsForHash().put(shareNewTargetKey, "unReadMsg", "1");
                redisSyncScheduler.markingDirtyUserChat(shareNewSelfKey);
                redisSyncScheduler.markingDirtyUserChat(shareNewTargetKey);

                userChatRepository.saveAll(ucList);

                // Save And Broadcast
                messageRepository.save(MessageConverter.toShareMessage(user, savedChat, course));
//                savedChat.updateLastReceivedMsg("산책 코스를 공유하였습니다");
                String shareNewChatKey = "chat:" + savedChat.getId();
                redisTemplate.opsForHash().put(shareNewChatKey, "lastReceivedMsg", "산책 코스를 공유하였습니다");
                redisSyncScheduler.markingDirtyChat(shareNewChatKey);
                redisTemplate.opsForHash().increment(shareNewTargetKey, "unReadMsg", 1);

                PayloadDTO<Object> payloadDTO = PayloadDTO.builder()
                        .type("share")
                        .payload(MessageConverter.toBroadCastCourseDTO(user.getId(), savedChat.getId(), course))
                        .build();

                // 메세지 BroadCast
                redisPublisher.publishMsg("redis.chat.share." + reqDTO.getChatId(), payloadDTO);
                Counter.builder("chat_share_sent_total")
                        .description("MySQL 저장 + Redis publish까지 성공적으로 완료된 산책 코스 공유 수")
                        .register(meterRegistry)
                        .increment();
                sample.stop(Timer.builder("chat_share_processing_seconds")
                        .description("shareCourse() 진입부터 Redis publish 완료까지 서버 처리 시간(네트워크 왕복 제외)")
                        .register(meterRegistry));
            } else { // 친구 공유, 채팅이 존재하는 경우
                Long privateChatId = privateChat.getId();
                log.info("'shareCourse'/toFriend - privateChat is Not Null! id = {}", privateChatId);

                String key = "user:"+user.getId() + ":"+ privateChatId;
                redisTemplate.opsForHash().put(key,  "isChatting", "true");
                redisSyncScheduler.markingDirtyUserChat(key);

                // Save And Broadcast
                messageRepository.save(MessageConverter.toShareMessage(user, privateChat, course));

                List<Long> userChatList = userChatRepository.findAllByChat_Id(privateChatId);

                userChatList.forEach(userChat -> {
                    String thisKey = "user:" + userChat + ":" + privateChatId;
                    String isChatting = redisTemplate.opsForHash().get(thisKey, "isChatting").toString();
                    if (isChatting.equals("false")) {
                        redisTemplate.opsForHash().increment(thisKey, "unReadMsg", 1);
                        redisSyncScheduler.markingDirtyUserChat(thisKey);
                    }
                });

                String privateShareChatKey = "chat:" + privateChatId;
                redisTemplate.opsForHash().put(privateShareChatKey, "lastReceivedMsg", "산책 코스를 공유하였습니다");
                redisSyncScheduler.markingDirtyChat(privateShareChatKey);

                PayloadDTO<Object> payloadDTO = PayloadDTO.builder()
                        .type("share")
                        .payload(MessageConverter.toBroadCastCourseDTO(user.getId(), privateChatId, course))
                        .build();

                // 메세지 BroadCast
                redisPublisher.publishMsg("redis.chat.share." + reqDTO.getChatId(), payloadDTO);
                Counter.builder("chat_share_sent_total")
                        .description("MySQL 저장 + Redis publish까지 성공적으로 완료된 산책 코스 공유 수")
                        .register(meterRegistry)
                        .increment();
                sample.stop(Timer.builder("chat_share_processing_seconds")
                        .description("shareCourse() 진입부터 Redis publish 완료까지 서버 처리 시간(네트워크 왕복 제외)")
                        .register(meterRegistry));
            }
        }


    }
}
