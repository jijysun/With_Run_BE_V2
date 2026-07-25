package UMC_8th.With_Run.global.security.jwt;

import UMC_8th.With_Run.global.config.properties.Constants;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class StompChannelInterceptor implements ChannelInterceptor {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        // 1. 들어온 메시지가 STOMP 커맨드(CONNECT/SUBSCRIBE/SEND/DISCONNECT) 확인
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

        // 2. CONNECT 프레임 검증
        // - SUBSCRIBE/SEND는 검증 X = 3~5번인 CONNECT 시점의 세션에 심어둔 Principal이 같은 커넥션의 이후 모든 프레임에 자동으로 연결!
        // =재검증 불필요, 커넥션 단위 신뢰
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {

            // 3. CONNECT 프레임의 네이티브 헤더에서 토큰 꺼내기
            String token = resolveToken(accessor);

            // 4. Token is Null Or 위조/만료 == EX 터뜨리기
            if (!StringUtils.hasText(token) || !jwtTokenProvider.validateToken(token)) {
                throw new MessagingException("STOMP CONNECT 인증 실패: 유효한 JWT가 필요합니다.");
            }

            // 5. 검증된 토큰에서 Authentication(email 기반 Principal) 제작 + 세션에 등록
            // - 이 시점 이후: 같은 커넥션의 SEND/SUBSCRIBE 프레임의 Principal이 자동 주입
            /// 컨트롤러의 @MessageMapping 메서드가 Principal 로 쓰기
            Authentication authentication = jwtTokenProvider.getAuthentication(token);
            accessor.setUser(authentication);
        }

        return message;
    }

    private String resolveToken(StompHeaderAccessor accessor) {
        String bearerToken = accessor.getFirstNativeHeader(Constants.AUTH_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(Constants.TOKEN_PREFIX)) {
            return bearerToken.substring(Constants.TOKEN_PREFIX.length());
        }
        return null;
    }
}
