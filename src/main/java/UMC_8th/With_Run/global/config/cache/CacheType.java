package UMC_8th.With_Run.global.config.cache;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.Duration;

/**
 * 1. USER: chatting()의 발신자 조회(email -> User)용. 핵심 신원 데이터는 실사용 중 거의 안 바뀌므로 TTL을 길게(1시간)
 * 2. PROFILE : 이름·프로필 사진처럼 사용자가 REST API로 비교적 자주 바꿀 수 있는 값. User보다 짧은 TTL(10분)
 *    — (2026-08-05) `ProfileServiceImpl.updateProfile()`/`UserServiceImpl.cancelMembership()`에 명시적
 *    캐시 evict를 추가해, 변경 자체는 TTL과 무관하게 즉시 반영됨. TTL은 이제 "안 바뀐 값을 얼마나 오래
 *    재사용할지"만 결정하는 순수 성능 파라미터라 3분 부하테스트 안에서 재만료되지 않을 만큼 넉넉히 늘림.
 *
 * cacheName: @Cacheable(cacheNames = ...)이 참조하는 실제 캐시 저장소 이름이고,
 * maxSize: Caffeine이 이 캐시에 얼마나 많은 항목까지 들고 있을지의 상한
 * (없으면 이론상 유저 수만큼 계속 늘어날 수 있어 반드시 지정 — 상한을 넘으면 Caffeine의 W-TinyLFU 정책이 접근 빈도가 낮은 항목부터 밀어낸다).
 */
@Getter
@RequiredArgsConstructor
public enum CacheType {

    USER("userCache", Duration.ofHours(1), 1_000),
    PROFILE("profileCache", Duration.ofMinutes(10), 1_000);

    private final String cacheName;
    private final Duration ttl;
    private final long maxSize;
}
