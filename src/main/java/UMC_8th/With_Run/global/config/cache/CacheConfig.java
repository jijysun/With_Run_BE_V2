package UMC_8th.With_Run.global.config.cache;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Spring의 CaffeineCacheManager는 매니저 하나에 캐시 스펙(TTL 등)을 하나만 적용한다.
 * CacheType별로 서로 다른 TTL을 줘야 해서, CacheType 값 하나당 Caffeine 네이티브 캐시를
 * 직접 만들고(각자 다른 expireAfterWrite/maximumSize), 그 목록을 SimpleCacheManager에
 * 등록하는 방식으로
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager cacheManager = new SimpleCacheManager();

        List<CaffeineCache> caches = Arrays.stream(CacheType.values())
                .map(cacheType -> new CaffeineCache(
                        cacheType.getCacheName(),
                        Caffeine.newBuilder()
                                .expireAfterWrite(cacheType.getTtl())
                                .maximumSize(cacheType.getMaxSize())
                                .recordStats() // 히트/미스 통계 — 필요하면 나중에 Micrometer로 노출 가능
                                .build()))
                .collect(Collectors.toList());

        cacheManager.setCaches(caches);
        return cacheManager;
    }
}
