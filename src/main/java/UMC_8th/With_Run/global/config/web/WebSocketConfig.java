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

    // (2026-08-06) 200VU 실측에서 인바운드 활성 스레드가 접속 버스트 구간에 정확히 10(=당시 core 상한)을 찍고
    // 큐에 17이 쌓였다 — core 10이 그 순간엔 실제 제약이었다는 뜻. 정상 구간은 3~4개로 충분했지만,
    // "core가 실제로 바인딩 조건이었는지"를 확인하기 위해 10→12로만 올려 재측정한다.
    // (12에서도 활성 최대가 10 언저리면 10으로 충분했던 것, 11~12까지 차면 10이 부족했던 것 — 어느 쪽이든 해석 가능)
    // 아웃바운드는 200VU에서 활성/큐 모두 사실상 0이었으므로 배포본(12/36) 그대로 두고 인바운드만 단일 변수로 바꾼다.
    @Bean
    public ThreadPoolTaskExecutor stompInboundTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(12);
        executor.setMaxPoolSize(24);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("stomp-inbound-");
        executor.initialize();
        ExecutorServiceMetrics.monitor(meterRegistry, executor.getThreadPoolExecutor(), "stomp.inbound");
        return executor;
    }

    // 이번 회차 미변경(단일 변수 유지) — 200VU에서 활성 스레드·큐가 모두 0에 가까워 조정 근거가 없음.
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
