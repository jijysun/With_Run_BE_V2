package UMC_8th.With_Run.global.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@Getter
@Setter
@ConfigurationProperties("observer")
public class ObserverProperties {
    // 채팅 관측 대시보드(FE)가 /api/observer/token 호출 시 제시해야 하는 공유 시크릿.
    // 실제 유저 로그인 없이 관찰자 전용 JWT를 발급하기 위한 최소한의 게이트키퍼.
    private String accessKey = "";
}
