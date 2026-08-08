package UMC_8th.With_Run.domain.chat.service.impl.messaging;

import UMC_8th.With_Run.domain.chat.converter.ChatConverter;
import UMC_8th.With_Run.domain.chat.converter.MessageConverter;
import UMC_8th.With_Run.domain.chat.converter.UserChatConverter;
import UMC_8th.With_Run.domain.chat.dto.ChatRequestDTO;
import UMC_8th.With_Run.domain.chat.dto.GPTDTO;
import UMC_8th.With_Run.domain.chat.dto.MessagePersistResultDTO;
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
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
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
    // RedisPublisher와 동일한 Spring 관리 빈을 써야 함 — new ObjectMapper()는 JavaTimeModule이 없어
    // PayloadDTO의 LocalDateTime(createdAt) 직렬화 시 InvalidDefinitionException이 남.
    private final ObjectMapper objectMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final MeterRegistry meterRegistry;
    private final RedisSyncScheduler redisSyncScheduler;
    private final RedisScript<Long> unreadIncrementScript;
    private final UserService userService;

    @Value("${chatgpt.api.key}")
    private String API_KEY;

    @Value("${chatgpt.api.uri}")
    private String API_URI;

    // register() 비용(Meter.Id 생성 + 레지스트리 조회)을 메시지마다 반복하지 않도록 필드로 한 번만 등록.
    private Timer chatMessageProcessingTimer;

    @PostConstruct
    private void initMetrics() {
        chatMessageProcessingTimer = Timer.builder("chat_message_processing_seconds")
                .description("persistMessage() 진입부터 Redis publish 완료까지 서버 처리 시간(네트워크 왕복 제외)")
                .register(meterRegistry);
    }


    /**
     *
     * 채팅 메서드 — MySQL 단계(persistMessage) / Redis 단계(publishToRedis)로 분리.
     * Controller(ChatController.chattingWithRedis)가 두 메서드를 순차 호출한다.
     * 이유: 같은 메서드 안에서 @Transactional을 걸면 Redis I/O(왕복 4~5회) 동안 DB 커넥션을 계속 붙잡게 되어
     * (Hibernate 기본 커넥션 모드 DELAYED_ACQUISITION_AND_HOLD = 트랜잭션 종료 시점에 반납) HikariCP 풀이 빨리 고갈됨.
     * persistMessage()가 반환(=트랜잭션 종료=커넥션 반납)한 뒤에 publishToRedis()를 호출해 점유 시간을 최소화한다.
     * (AFTER_COMMIT 이벤트 리스너 방식은 커넥션 반납 이전에 실행되어 이 문제를 해결하지 못함 — 검토 후 기각.)
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
    public MessagePersistResultDTO persistMessage(Long chatId, ChatRequestDTO.ChattingReqDTO reqDTO, String authenticatedEmail) {
        // 서버 사이드 순수 처리시간( 수신 ~ Redis publish 완료)만 측정
        // — 클라이언트-서버 왕복(k6 correlation ID)과 구분해서, 병목이 서버 처리 자체인지 네트워크 왕복인 지 확인할 수 있게끔.
        // publishToRedis() 종료 시점에 stop — 두 단계를 합친 것이 실제 "서버 처리시간".
        Timer.Sample sample = Timer.start(meterRegistry);

        // 발신자 조회: reqDTO.getUserId() ->  STOMP CONNECT에서 검증된 authenticatedEmail로만 조회 = userId 위조 가능
        // User/Profile을 Caffeine 캐시(userCache/profileCache, 서로 다른 TTL)로 분리 조회 —
        // Cache Hit = SQL 자체가 안나감, 로그가 나가도 SQL이 발생한 건 아님.
        log.debug("SELECT 시도(Cache HIT 시 SQL 생략) -> persistMessage(): User (email={})", authenticatedEmail);
        User user = userService.getCachedUser(authenticatedEmail);
        log.debug("SELECT 시도(Cache HIT 시 SQL 생략) -> persistMessage(): Profile (userId={})", user.getId());
        Profile profile = userService.getCachedProfile(user.getId());

        log.debug("SELECT 시도 (existsById) -> persistMessage(): (chatId:{})", chatId);
        if (!chatRepository.existsChatById(chatId)){
            throw new ChatHandler(ErrorCode.WRONG_CHAT);
        }

        log.debug("SQL 발생! -> persistMessage(): Get Chat (chatId={})", chatId);
//        Chat chat = chatRepository.findById(chatId).orElseThrow(() -> new ChatHandler(ErrorCode.EMPTY_CHAT_LIST));
        Chat chat = chatRepository.getReferenceById(chatId);

        /// 메세지 객체 하나만 저장하는데 왜 리스트화 하는 것?
        Message msg = MessageConverter.toMessage(user, chat, reqDTO);
        List<Message> messageList = new ArrayList<>();
        messageList.add(msg);

        // 안읽은 메세지 기능 대상자 목록 — 실제 increment(Redis)는 publishToRedis()에서 처리.
        log.debug("SQL 발생! -> persistMessage(): SELECT UserChatList (chatId={})", chatId);
        List<Long> userChatList = userChatRepository.findAllByChat_Id(chatId);

        log.debug("SQL 발생! -> persistMessage(): INSERT Message (chatId={}, userId={})", chatId, user.getId());
        messageRepository.saveAll(messageList);
//        chat.updateLastReceivedMsg(reqDTO.getMessage());

        return new MessagePersistResultDTO(user.getId(), profile, msg, userChatList, sample);
    }

    @Override
    public void publishToRedis(Long chatId, MessagePersistResultDTO result) {
        List<Long> userChatList = result.userChatList();
        String chatKey = "chat:" + chatId;
        String publishTopic = "redis.chat.msg." + chatId;

        PayloadDTO<Object> payloadDTO = PayloadDTO.builder() // redis 처리 전용 dto 변환,
                .type("chat")
                // user.getProfile()은 더 이상 쓰면 안 됨 — user가 findByEmail(join fetch 없음)로 조회됨
                // + profile은 지연 로딩 프록시고, 캐시로 재사용되는 detached 상태라 세션이 없어 LazyInitializationException이 남.
                // 대신 별도로 캐시 조회한 profile을 그대로 사용.
                .payload(MessageConverter.toBroadCastMsgDTO(result.userId(), chatId, result.profile(), result.message()))
                .build();

        // PUBLISH할 JSON은 파이프라인 진입 전에 미리 직렬화 — SessionCallback.execute()는 checked exception을
        // 던질 수 없어서(DataAccessException만 선언됨), JsonProcessingException은 여기서 먼저 처리해야 함.
        String payloadJson;
        try {
            payloadJson = objectMapper.writeValueAsString(payloadDTO);
        } catch (JsonProcessingException e) {
            throw new ChatHandler(ErrorCode.MSG_SERIALIZE_FAIL);
        }

        log.debug("Redis PipeLine I/O 발생! -> publishToRedis(): EVAL(unreadIncrement) × {}건 + HPUT + SADD + PUBLISH, 파이프라인으로 1회 왕복 처리", userChatList.size());
        // SessionCallback: RedisConnection을 직접 다루지 않고, RedisOperations의 타입-세이프 API(execute(script, keys, args)) 사용 가능!
        redisTemplate.executePipelined(new SessionCallback<Object>() { // pipeLined = RTT 개선 부여
            @Override
            public Object execute(RedisOperations operations) {
                userChatList.forEach(userChat -> {
                    String key = "user:" + userChat + ":" + chatId;
                    // .lua = 원자성 부여
                    operations.execute(unreadIncrementScript, List.of(key, RedisSyncScheduler.DIRTY_USER_CHAT_KEY), "1");
                });

                operations.opsForHash().put(chatKey, "lastReceivedMsg", result.message().getMsg());
                operations.opsForSet().add(RedisSyncScheduler.DIRTY_CHAT_KEY, chatKey);
                operations.convertAndSend(publishTopic, payloadJson);
                return null;
            }
        });

        result.sample().stop(chatMessageProcessingTimer);
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
            }
        }


    }
}
