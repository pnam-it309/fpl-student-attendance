package udpm.hn.studentattendance.infrastructure.config.redis;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import io.lettuce.core.ReadFrom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

@Configuration
@EnableCaching
public class RedisTemplateConfig {

    @Value("${spring.data.redis.host}")
    private String redisHost;

    @Value("${spring.data.redis.port}")
    private String redisPort;

    @Value("${spring.data.redis.url:}")
    private String redisUrl;

    @Value("${spring.cache.redis.time-to-live}")
    private long redisTTL;

    private final ObjectMapper objectMapper;

    private static final Logger logger = LoggerFactory.getLogger(RedisTemplateConfig.class);

    @Autowired
    public RedisTemplateConfig(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Bean
    public LettuceConnectionFactory jedisConnectionFactory() {
        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
                .readFrom(ReadFrom.REPLICA_PREFERRED)
                .build();

        RedisStandaloneConfiguration serverConfig;
        if (org.springframework.util.StringUtils.hasText(redisUrl)) {
            serverConfig = new RedisStandaloneConfiguration();
            try {
                String cleanUrl = redisUrl.replace("redis://", "");
                if (cleanUrl.contains("@")) {
                    cleanUrl = cleanUrl.split("@")[1];
                }
                if (cleanUrl.contains(":")) {
                    String[] parts = cleanUrl.split(":");
                    serverConfig.setHostName(parts[0]);
                    serverConfig.setPort(Integer.parseInt(parts[1]));
                } else {
                    serverConfig.setHostName(cleanUrl);
                    serverConfig.setPort(6379);
                }
            } catch (Exception e) {
                logger.error("Failed to parse Redis URL: " + redisUrl + ", falling back to host/port");
                serverConfig = new RedisStandaloneConfiguration(redisHost, Integer.parseInt(redisPort));
            }
        } else {
            serverConfig = new RedisStandaloneConfiguration(redisHost, Integer.parseInt(redisPort));
        }

        LettuceConnectionFactory factory = new LettuceConnectionFactory(serverConfig, clientConfig);
        factory.afterPropertiesSet();
        try {
            factory.getConnection().ping();
            logger.info("Redis connected to " + serverConfig.getHostName() + ":" + serverConfig.getPort());
        } catch (Exception e) {
            logger.warn("Redis connection failed: " + e.getMessage());
        }
        return factory;
    }

    @Bean
    public ObjectMapper redisObjectMapper() {
        ObjectMapper redisObjectMapper = objectMapper.copy();

        // Sử dụng modern approach thay vì deprecated enableDefaultTyping
        redisObjectMapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL);

        // Configure to handle proxy objects and complex types
        redisObjectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        redisObjectMapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
        redisObjectMapper.configure(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES, false);
        redisObjectMapper.configure(DeserializationFeature.FAIL_ON_NULL_CREATOR_PROPERTIES, false);

        // Handle proxy objects
        redisObjectMapper.configure(DeserializationFeature.FAIL_ON_INVALID_SUBTYPE, false);

        // Additional configurations for JPA entities and proxies
        redisObjectMapper.configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);
        redisObjectMapper.configure(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL, true);

        return redisObjectMapper;
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate() {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(jedisConnectionFactory());
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());

        // Sử dụng GenericJackson2JsonRedisSerializer để tránh deprecated
        // setObjectMapper
        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(redisObjectMapper());
        redisTemplate.setValueSerializer(serializer);
        redisTemplate.setHashValueSerializer(serializer);

        return redisTemplate;
    }

    @Bean
    public RedisCacheManager cacheManager(LettuceConnectionFactory connectionFactory) {
        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(redisObjectMapper());

        RedisCacheConfiguration cacheConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofSeconds(redisTTL))
                .disableCachingNullValues()
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(serializer));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(cacheConfig)
                .build();
    }
}