package UMC_8th.With_Run.global.config.web;

import UMC_8th.With_Run.global.security.jwt.StompChannelInterceptor;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.jvm.ExecutorServiceMetrics;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    // WebSocket 통신 설정 파일
    // 이후 Common/config로 옮길 예정입니다.

    // STOMP CONNECT 프레임 인증용 인터셉터(아래 configureClientInboundChannel에서 등록)
    private final StompChannelInterceptor stompChannelInterceptor;
    private final MeterRegistry meterRegistry;

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

    // 클라이언트 → 서버 → All STOMP 프레임이 이 채널을 거침
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // StompChannelInterceptor가 CONNECT 프레임에서만 JWT를 검증하고 세션에 Principal을 심는다.
        registration.interceptors(stompChannelInterceptor);
        registration.taskExecutor(stompInboundTaskExecutor());
    }

    // 서버 → 클라이언트로 나가는 브로드캐스트(RedisSubscriber.onMessage -> convertAndSend)가 거치는 채널
    @Override
    public void configureClientOutboundChannel(ChannelRegistration registration) {
        registration.taskExecutor(stompOutboundTaskExecutor());
    }

    // (2026-08-05) 12/24로 올렸던 1차 재조정은 개선 없이 스레드만 폭증(Peak 279 — 07-29 "너무 컸던" 최초 시도와
    // 동일 패턴). 그 회차 진짜 원인은 풀 크기가 아니라 캐시 콜드스타트발 HikariCP 몰림(Pending mean 21.5/max 199)
    // 으로 재진단됨 — 풀 크기 자체는 결정적 변수가 아니었다고 보고 소폭만 축소.
    @Bean
    public ThreadPoolTaskExecutor stompInboundTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("stomp-inbound-");
        executor.initialize();
        ExecutorServiceMetrics.monitor(meterRegistry, executor.getThreadPoolExecutor(), "stomp.inbound");
        return executor;
    }

    // 코어는 소폭만 축소하되, 순간 팬아웃 버스트에 대응할 여유(max)는 core의 3배로 넉넉히 유지.
    @Bean
    public ThreadPoolTaskExecutor stompOutboundTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(12);
        executor.setMaxPoolSize(36);
        executor.setQueueCapacity(1000);
        executor.setThreadNamePrefix("stomp-outbound-");
        executor.initialize();
        ExecutorServiceMetrics.monitor(meterRegistry, executor.getThreadPoolExecutor(), "stomp.outbound");
        return executor;
    }
}
