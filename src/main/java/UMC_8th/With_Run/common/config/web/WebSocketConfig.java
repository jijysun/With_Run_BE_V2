package UMC_8th.With_Run.common.config.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    // WebSocket 통신 설정 파일
    // 이후 Common/config로 옮길 예정입니다.

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 통신 담당 엔드포인트 지정
        // front 측에서 Back 과 연결하고 싶은 경우 이로 요청, Not Http!
        registry.addEndpoint("/api/ws").setAllowedOrigins("*");
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {

        // MessageMapping 으로 온 URL 앞에 /sub을 붙힘! + MessageBrocker 가 이를 가로채 처리
        // 이후 해당 토픽을 구독한 클라이언트에게 Broadcast
        registry.enableSimpleBroker("/sub");
        registry.setApplicationDestinationPrefixes("/pub");

    }
}
