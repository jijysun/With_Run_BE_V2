package UMC_8th.With_Run.common.redis.pub_sub;

import UMC_8th.With_Run.common.apiResponse.status.ErrorCode;
import UMC_8th.With_Run.common.exception.handler.ChatHandler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RedisPublisher {

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public <T> void publishMsg (String topic, T messageDTO){ // 일반 메세지, 공유 메세지 둘 다 사용 예정
        try {
            String msg = objectMapper.writeValueAsString(messageDTO);
            stringRedisTemplate.convertAndSend(topic, msg);
        } catch (JsonProcessingException e) {
            throw new ChatHandler(ErrorCode.MSG_SERIALIZE_FAIL);
        }

    }
}
