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

    // 첫 200VU 실측: CPU(t3.micro, 2vCPU)가 이미 포화 상태인데 풀을 넓혀서 스레드 폭증 + Load Average·HikariCP Pending 악화
    // - CPU-bound 구간에서는 스레드를 늘려도 처리량이 안 느는데 경쟁만 늘어남.
    // + 보수적으로 축소. Micrometer로 계측해 Tomcat 스레드와 분리해서 관찰 하기
    @Bean
    public ThreadPoolTaskExecutor stompInboundTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(6);
        executor.setMaxPoolSize(12);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("stomp-inbound-");
        executor.initialize();
        ExecutorServiceMetrics.monitor(meterRegistry, executor.getThreadPoolExecutor(), "stomp.inbound");
        return executor;
    }

    // 실측상 InBound 보다 나가는 량이 더 많아서(팬아웃 약 4배) 조금 더 크게 — 단 이전(24/48)보다는 축소.
    @Bean
    public ThreadPoolTaskExecutor stompOutboundTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(1000);
        executor.setThreadNamePrefix("stomp-outbound-");
        executor.initialize();
        ExecutorServiceMetrics.monitor(meterRegistry, executor.getThreadPoolExecutor(), "stomp.outbound");
        return executor;
    }
}
