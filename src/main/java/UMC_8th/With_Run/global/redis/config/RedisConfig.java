package UMC_8th.With_Run.global.redis.config;

import UMC_8th.With_Run.global.redis.pub_sub.RedisSubscriber;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Value("${spring.data.redis.host}")
    private String host;

    @Value("${spring.data.redis.port}")
    private int port;

    @Bean
    public RedisConnectionFactory lettuceConnectionFactory() {
        return new LettuceConnectionFactory(new RedisStandaloneConfiguration(host, port));
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate() {

        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(lettuceConnectionFactory());

        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new StringRedisSerializer());

        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashValueSerializer(new StringRedisSerializer());

        return redisTemplate;
    }

    // Lua Script 전용 RedisTemplate
    @Bean
    public StringRedisTemplate stringRedisTemplate() {
        return new StringRedisTemplate(lettuceConnectionFactory());
    }

    // 안읽음 카운트 원자적 증가 스크립트 빈 등록.
    @Bean
    public RedisScript<Long> unreadIncrementScript() {
        return RedisScript.of(new ClassPathResource("scripts/unread_increment.lua"), Long.class);
    }


    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            MessageListenerAdapter chatListenerAdapter,
            MessageListenerAdapter shareListenerAdapter
    ){
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(chatListenerAdapter, new PatternTopic("redis.chat.msg.*"));
        container.addMessageListener(shareListenerAdapter, new PatternTopic("redis.chat.share.*"));
        return container;
    }

    @Bean
    public MessageListenerAdapter chatListenerAdapter(RedisSubscriber subscriber) {
        return new MessageListenerAdapter(subscriber, "onMessage");
    }

    @Bean
    public MessageListenerAdapter shareListenerAdapter(RedisSubscriber subscriber) {
        return new MessageListenerAdapter(subscriber, "onMessage");
    }
}