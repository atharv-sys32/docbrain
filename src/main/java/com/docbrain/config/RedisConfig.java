package com.docbrain.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.net.URI;

@Configuration
public class RedisConfig {

    @Value("${spring.data.redis.url:}")
    private String redisUrl;

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    @Bean
    public LettuceConnectionFactory redisConnectionFactory() {
        String cleanUrl = redisUrl;
        if (cleanUrl != null) {
            cleanUrl = cleanUrl.trim();
            // Strip leading/trailing double quotes if they exist
            if (cleanUrl.startsWith("\"") && cleanUrl.endsWith("\"")) {
                cleanUrl = cleanUrl.substring(1, cleanUrl.length() - 1);
            }
            // Strip leading/trailing single quotes if they exist
            if (cleanUrl.startsWith("'") && cleanUrl.endsWith("'")) {
                cleanUrl = cleanUrl.substring(1, cleanUrl.length() - 1);
            }
            cleanUrl = cleanUrl.trim();
        }

        if (cleanUrl != null && !cleanUrl.isEmpty()) {
            URI uri = URI.create(cleanUrl);
            RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
            config.setHostName(uri.getHost());
            config.setPort(uri.getPort());
            if (uri.getUserInfo() != null) {
                String[] userInfo = uri.getUserInfo().split(":", 2);
                if (userInfo.length == 2) {
                    config.setPassword(userInfo[1]);
                } else {
                    config.setPassword(userInfo[0]);
                }
            }

            boolean useSsl = cleanUrl.startsWith("rediss://");
            LettuceClientConfiguration.LettuceClientConfigurationBuilder builder =
                    LettuceClientConfiguration.builder();
            if (useSsl) {
                builder.useSsl().disablePeerVerification();
            }
            return new LettuceConnectionFactory(config, builder.build());
        }

        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(redisHost, redisPort);
        if (redisPassword != null && !redisPassword.isEmpty()) {
            config.setPassword(redisPassword);
        }
        return new LettuceConnectionFactory(config);
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }
}
