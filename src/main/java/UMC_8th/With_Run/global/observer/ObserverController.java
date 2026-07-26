package UMC_8th.With_Run.global.observer;

import UMC_8th.With_Run.global.apiResponse.StndResponse;
import UMC_8th.With_Run.global.apiResponse.status.ErrorCode;
import UMC_8th.With_Run.global.apiResponse.status.SuccessCode;
import UMC_8th.With_Run.global.config.properties.JwtProperties;
import UMC_8th.With_Run.global.config.properties.ObserverProperties;
import UMC_8th.With_Run.global.exception.handler.ObserverHandler;
import UMC_8th.With_Run.global.observer.dto.ObserverResponseDTO;
import UMC_8th.With_Run.global.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 채팅 관측 대시보드(FE, 별도 세션/레포)가 STOMP CONNECT에 쓸 JWT 발급 전용 엔드포인트.
 * 실제 유저 로그인 플로우를 태우지 않는 이유: 관찰자는 DB에 존재하는 User가 아니라
 * "모든 방을 구독만 하는" 고정 계정이며, 목적은 오직 JWT_SECRET_KEY를 서버 밖(브라우저)으로
 * 내보내지 않는 것 하나뿐이다 — SecurityConfig에서 이 경로는 permitAll, 대신 별도 공유 시크릿
 * (X-Observer-Key)으로 게이트를 건다.
 */
@RestController
@RequestMapping("/api/observer")
@RequiredArgsConstructor
public class ObserverController {

    private static final String OBSERVER_KEY_HEADER = "X-Observer-Key";
    private static final String OBSERVER_EMAIL = "observer@with-run.internal";

    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;
    private final ObserverProperties observerProperties;

    @PostMapping("/token")
    public StndResponse<ObserverResponseDTO.IssueTokenDTO> issueToken(
            @RequestHeader(value = OBSERVER_KEY_HEADER, required = false) String key) {

        String expectedKey = observerProperties.getAccessKey();
        if (!StringUtils.hasText(expectedKey) || !expectedKey.equals(key)) {
            throw new ObserverHandler(ErrorCode.OBSERVER_UNAUTHORIZED);
        }

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                OBSERVER_EMAIL, null, List.of(new SimpleGrantedAuthority("ROLE_OBSERVER")));
        String accessToken = jwtTokenProvider.generateToken(authentication);

        ObserverResponseDTO.IssueTokenDTO result = ObserverResponseDTO.IssueTokenDTO.builder()
                .accessToken(accessToken)
                .expiresIn(jwtProperties.getExpiration().getAccess())
                .build();

        return StndResponse.onSuccess(result, SuccessCode.OBSERVER_TOKEN_ISSUE_SUCCESS);
    }
}
