package com.example.demo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;

@Configuration
//@EnableRedisRepositories //Redis에 대한 CRUD를 수행해야 한다면 넣어줘야 한다.
public class RedisConfig {

    @Value("${spring.data.redis.host}")
    private String host;

    @Value("${spring.data.redis.port}")
    private int port;

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        return new LettuceConnectionFactory(host, port);
    }

    /*
    * Redis Cache를 사용하는 방법으로는 크게 RedisRepository와 RedisTemplate이 있다.
    * 그중 이번에 사용할 RedisTmplate은 특정 Entity뿐만 아니라 원하는 여러가지 타입(자료형)을 넣을 수 있다.
    * RedisTemplate를 선언한 후 원하는 key, Value 타입에 맞게 Operations(String, Hash, Set 등)를 선언하여 사용할 수 있다는 장점이 있다.
    * 아래와 같이 제네릭을 <?, ?>으로 선언하여 RedisTemplate을 주입받을 때 원하는 key, value 타입을 지정하여 주입받을 수 있게 한다.
    * https://velog.io/@rnqhstlr2297/Spring-boot%EC%97%90%EC%84%9C-Redis-%EC%82%AC%EC%9A%A9%EB%B2%95
    * */
    @Bean
    public RedisTemplate<?, ?> redisTemplate() {
        RedisTemplate<?, ?> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory());
        return redisTemplate;
    }
}
