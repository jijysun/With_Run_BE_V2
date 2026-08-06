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

    // (2026-08-05) 풀 크기 튜닝 중단 —  세 값 모두 가 270~280대에서
    // - 6/12: Threads Peak + Metric 수집 실패
    // - 12/24: Threads Peak + Metric 수집 실패
    // - 10/20: Threads Peak + Metric 수집 실패
    // 결정적 변수는 아닌 듯
    //
    // 그래서 값은 Spring 기본값을 그대로 + 빈으로 직접 등록해 Micrometer 계측만
    // - corePoolSize = 가용 프로세서 × 2 (Spring TaskExecutorRegistration 기본값과 동일)
    // - setMaxPoolSize / setQueueCapacity는 의도적으로 호출하지 않음 = 큐 사이즈는 무제한이라 Spring 동작 그대로
    // - allowCoreThreadTimeOut = true 역시 Spring 기본값과 동일
    // 실제로 기본값이 재현됐는지는 Grafana "코어/맥스 PoolSize 설정 값" 패널로 직접 검증할 것
    @Bean
    public ThreadPoolTaskExecutor stompInboundTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(Runtime.getRuntime().availableProcessors() * 2);
        executor.setAllowCoreThreadTimeOut(true);
        executor.setThreadNamePrefix("stomp-inbound-");
        executor.initialize();
        ExecutorServiceMetrics.monitor(meterRegistry, executor.getThreadPoolExecutor(), "stomp.inbound");
        return executor;
    }

    // 아웃바운드도 동일하게 Spring 기본값 재현 + 계측만 부착
    @Bean
    public ThreadPoolTaskExecutor stompOutboundTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(Runtime.getRuntime().availableProcessors() * 2);
        executor.setAllowCoreThreadTimeOut(true);
        executor.setThreadNamePrefix("stomp-outbound-");
        executor.initialize();
        ExecutorServiceMetrics.monitor(meterRegistry, executor.getThreadPoolExecutor(), "stomp.outbound");
        return executor;
    }
}
