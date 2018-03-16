package com.github.fnpac.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

/**
 * Created by 刘春龙 on 2018/3/15.
 */
@Component
@PropertySource("classpath:redis.properties")
public class RedisProperties {

    /**
     * Enable SSL.
     */
    @Value("${redis.ssl:false}")
    private boolean ssl;

    /**
     * Database index used by the connection factory.
     */
    @Value("${redis.database:0}")
    private int database;

    /**
     * Connection timeout in milliseconds.
     */
    @Value("${redis.timeout:-1}")
    private int timeout;

    /**
     * Redis url, which will overrule host, port and password if set.
     */
    @Value("${redis.url:Null}")
    private String url;

    /**
     * Redis server host.
     */
    @Value("${redis.host:localhost}")
    private String host;

    /**
     * Redis server port.
     */
    @Value("${redis.port:6379}")
    private int port;

    /**
     * Login password of the redis server.
     */
    @Value("${redis.password:Null}")
    private String password;

    public boolean isSsl() {
        return this.ssl;
    }

    public int getDatabase() {
        return this.database;
    }

    public int getTimeout() {
        return this.timeout;
    }

    public String getUrl() {
        return this.url;
    }

    public String getHost() {
        return this.host;
    }

    public int getPort() {
        return this.port;
    }

    public String getPassword() {
        return this.password;
    }
}
