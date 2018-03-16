package com.github.fnpac.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.StringUtils;
import redis.clients.jedis.JedisPoolConfig;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;

/**
 * Created by 刘春龙 on 2018/3/15.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    private final RedisProperties properties;

    @Autowired
    public CacheConfig(RedisProperties redisProperties) {
        this.properties = redisProperties;
    }

    @Bean
    public CacheManager cacheManager(RedisTemplate redisTemplate) {
        RedisCacheManager redisCacheManager = new RedisCacheManager(redisTemplate);
        redisCacheManager.setUsePrefix(true);
        return redisCacheManager;
    }

    /**
     * JedisConnectionFactory implements InitializingBean, DisposableBean, RedisConnectionFactory
     *
     * @param redisConnectionFactory
     * @return
     */
    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, String> redisTemplate =
                new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }

//    @Bean
//    public JedisConnectionFactory redisConnectionFactory() {
//        JedisConnectionFactory jedisConnectionFactory =
//                new JedisConnectionFactory();
//        jedisConnectionFactory.afterPropertiesSet();
//        return jedisConnectionFactory;
//    }

    //////////////////////////////////////////////////////////////////////
    //  Simplified configuration version for RedisAutoConfiguration of Spring Boot
    //////////////////////////////////////////////////////////////////////
    @Bean
    public JedisConnectionFactory redisConnectionFactory()
            throws UnknownHostException {
        return applyProperties(createJedisConnectionFactory());
    }

    private JedisConnectionFactory createJedisConnectionFactory() {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        return new JedisConnectionFactory(poolConfig);
    }

    protected final JedisConnectionFactory applyProperties(
            JedisConnectionFactory factory) {
        configureConnection(factory);
        if (this.properties.isSsl()) {
            factory.setUseSsl(true);
        }
        factory.setDatabase(this.properties.getDatabase());
        if (this.properties.getTimeout() > 0) {
            factory.setTimeout(this.properties.getTimeout());
        }
        return factory;
    }

    private void configureConnection(JedisConnectionFactory factory) {
        if (StringUtils.hasText(this.properties.getUrl())) {
            configureConnectionFromUrl(factory);
        } else {
            factory.setHostName(this.properties.getHost());
            factory.setPort(this.properties.getPort());
            if (this.properties.getPassword() != null) {
                factory.setPassword(this.properties.getPassword());
            }
        }
    }

    private void configureConnectionFromUrl(JedisConnectionFactory factory) {
        String url = this.properties.getUrl();
        if (url.startsWith("rediss://")) {
            factory.setUseSsl(true);
        }
        try {
            URI uri = new URI(url);
            factory.setHostName(uri.getHost());
            factory.setPort(uri.getPort());
            if (uri.getUserInfo() != null) {
                String password = uri.getUserInfo();
                int index = password.indexOf(":");
                if (index >= 0) {
                    password = password.substring(index + 1);
                }
                factory.setPassword(password);
            }
        }
        catch (URISyntaxException ex) {
            throw new IllegalArgumentException("Malformed 'spring.redis.url' " + url,
                    ex);
        }
    }
}
