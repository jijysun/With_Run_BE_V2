package UMC_8th.With_Run.global.redis.pub_sub;

import UMC_8th.With_Run.global.redis.dto.PayloadDTO;
import UMC_8th.With_Run.domain.chat.dto.ChatResponseDTO;
import UMC_8th.With_Run.global.apiResponse.status.ErrorCode;
import UMC_8th.With_Run.global.exception.handler.ChatHandler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;

@Component
@Slf4j
@RequiredArgsConstructor
public class RedisSubscriber implements MessageListener {

    private final SimpMessagingTemplate msgTemplate;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;

    @Override
    public void onMessage(Message message, byte[] pattern) {

        String payload = new String(message.getBody(), StandardCharsets.UTF_8);
        PayloadDTO <LinkedHashMap<String, Object>> payloadDTO = null;
        try {
            payloadDTO = objectMapper.readValue(payload, new TypeReference<>() { // 중립적으로 받기에 위에서 타입 중립화!
            });
        } catch (JsonProcessingException e) {
            throw new ChatHandler(ErrorCode.REDIS_CANT_LISTEN_MSG);
        }

        switch (payloadDTO.getType()) {
            case "chat":
                ChatResponseDTO.BroadcastMsgDTO broadcastMsgDTO = objectMapper.convertValue(payloadDTO.getPayload(), ChatResponseDTO.BroadcastMsgDTO.class);
                log.debug("broadcast chat ! {}", broadcastMsgDTO.getChatId());
                msgTemplate.convertAndSend("/sub/"+broadcastMsgDTO.getChatId()+"/msg", broadcastMsgDTO); // broadcast

                // 1개의 방 대한 "발행" 횟수 — 구독자 수만큼 실제 전달된 건수는 아님!
                // (convertAndSend 한 번이 그 destination의 모든 구독자에게 팬아웃)
                Counter.builder("chat_messages_broadcast_total")
                        .description("Redis에서 수신해 STOMP로 브로드캐스트한 채팅 메시지 수")
                        .register(meterRegistry)
                        .increment();
                break;

            case "share": // 채팅방 공유, 사용자 공유 모두 포함
                ChatResponseDTO.BroadcastCourseDTO broadcastCourseDTO = objectMapper.convertValue(payloadDTO.getPayload(), ChatResponseDTO.BroadcastCourseDTO.class);
                msgTemplate.convertAndSend("/sub/"+broadcastCourseDTO.getChatId()+"/msg", broadcastCourseDTO);
                break;

            default:
                log.info("unknown chat ! {}", payloadDTO.getPayload());
                break;
        }
    }
}