package UMC_8th.With_Run.common.scheduler;

import UMC_8th.With_Run.chat.entity.Chat;
import UMC_8th.With_Run.chat.entity.mapping.UserChat;
import UMC_8th.With_Run.chat.repository.ChatRepository;
import UMC_8th.With_Run.chat.repository.UserChatRepository;
import UMC_8th.With_Run.common.apiResponse.status.ErrorCode;
import UMC_8th.With_Run.common.exception.handler.ChatHandler;
import jakarta.transaction.Transactional;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.*;

@Configuration
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class RedisSyncScheduler {

    private static final String DIRTY_USER_CHAT_KEY = "dirty:user_chat_key:";
    private static final String DIRTY_CHAT_KEY = "dirty:chat_key:";
    private final RedisTemplate<String, Object> redisTemplate;
    private final ChatRepository chatRepository;
    private final UserChatRepository userChatRepository;

    public void markingDirtyUserChat(String key) {
        redisTemplate.opsForSet().add(DIRTY_USER_CHAT_KEY, key);
    }

    public void markingDirtyChat(String key) {
        redisTemplate.opsForSet().add(DIRTY_CHAT_KEY, key);
    }

    /**
     * 매일 00시 정각에 Redis에 저장된 사항을 MySQL 에 동기화 작업을 실시합니다.
     *
     * @return null!
     */
    @Transactional
    @Scheduled(cron = "0 0 0 * * ?") // 매일 오전 00시 정각!
    public void synchronizingChatToDB() {
        log.info("synchronizingChatToDB started");

        Set<Object> organizeKeyList = redisTemplate.opsForSet().members(DIRTY_CHAT_KEY);

        if (organizeKeyList.isEmpty()) { // 변경된 값이 없다면?
            log.info("No change to synchronized!");
            log.info("synchronizingChatToDB end");
            return;
        }

        List<Long> chatIdList = organizeKeyList.stream()
                .map(key -> Long.parseLong(key.toString().split(":")[1]))
                .toList();

        Map<Long, String> toUpdateChat = new HashMap<>();
        for (Object obj : organizeKeyList) {
            String key = obj.toString();

            Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);

            // chat:123:lastReceivedMsg = ~~
            toUpdateChat.put(Long.parseLong(key.split(":")[1]), entries.get("lastReceivedMsg").toString());
        }

        List<Chat> chatList = chatRepository.findAllById(chatIdList);

        for (Chat chat : chatList) {
            String lastReceivedMsg = toUpdateChat.get(chat.getId());
            if (lastReceivedMsg != null) {
                chat.updateLastReceivedMsg(lastReceivedMsg);
            }
        }

        redisTemplate.delete(DIRTY_CHAT_KEY); // 작업 종료
        log.info("synchronized size: {}, synchronizingChatToDB end", organizeKeyList.size());
    }


    @Scheduled(cron = "0 10 0 * * ?") // 위 채팅 사항 업데이트 후 10분 뒤 업데이트
    @Transactional
    public void synchronizingUserChatToDB() {
        log.info("synchronizingUserChatToDB started");

        Set<Object> organizeKeyList = redisTemplate.opsForSet().members(DIRTY_USER_CHAT_KEY);

        if (organizeKeyList.isEmpty()) { // 변경된 값이 없다면?
            log.info("No change to synchronized!");
            log.info("synchronizingUserChatToDB end");
            return;
        }

        Set<Long> userIdList = new HashSet<>();
        Set<Long> chatIdList = new HashSet<>();

        for (Object o : organizeKeyList) {
            String[] split = o.toString().split(":");
            userIdList.add(Long.parseLong(split[1]));
            chatIdList.add(Long.parseLong(split[2]));
        }

        Map<String, Map<String, Object>> redisDataMap = new HashMap<>();

        for (Object key : organizeKeyList) {

            Map<Object, Object> entries = redisTemplate.opsForHash().entries(key.toString());

            Map<String, Object> valueMap = new HashMap<>();
            entries.forEach((k, v) -> valueMap.put(k.toString(), v));
            redisDataMap.put(key.toString(), valueMap);
        }

//        List<UserChat> userChatList = userChatRepository.findallByToUpdateList(toUpdateList);
        List<UserChat> userChatList2 = userChatRepository.findByIdsWithDetails(userIdList, chatIdList);

        for (UserChat userChat : userChatList2) {
            // userChat 업데이트
            Map<String, Object> redisValue = redisDataMap.get("user:" + userChat.getUser().getId() + ":" + userChat.getChat().getId());
            if (redisValue != null) {
                userChat.updateUserChat(Integer.parseInt(redisValue.get("unReadMsg").toString()), Boolean.parseBoolean(redisValue.get("isChatting").toString()));
            }

        }

        redisTemplate.delete(DIRTY_USER_CHAT_KEY); // 작업 종료
        log.info("synchronized size: {}, synchronizingUserChatToDB end", organizeKeyList.size());
    }
}
