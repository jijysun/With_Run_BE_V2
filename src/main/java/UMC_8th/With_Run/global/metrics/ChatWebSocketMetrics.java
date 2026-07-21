package UMC_8th.With_Run.global.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 1. STOMP 커넥션 생명주기 이벤트 구독 = 활성 세션 수를 Gauge로 노출한다.
 * - Spring의 WebSocketMessageBrokerStats은 30분 마다 로깅 = Micrometer 바인딩 X
 * = 세션 이벤트를 직접 구독하는 게 더...
 *
 * 2. SessionConnectedEvent = CONNECTED 프레임이 나간 시점(핸드셰이크 완료)에 발행
 * = SessionConnectEvent(CONNECT 수신 시점)보다 실제 활성 연결 수와 정확히 일치한다.
 *
 */
@Component
public class ChatWebSocketMetrics {

    private final AtomicInteger activeSessions = new AtomicInteger(0);

    public ChatWebSocketMetrics(MeterRegistry registry) {
        registry.gauge("chat_websocket_sessions_active", activeSessions);
    }

    @EventListener
    public void onConnected(SessionConnectedEvent event) {
        activeSessions.incrementAndGet();
    }

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        activeSessions.decrementAndGet();
    }
}
