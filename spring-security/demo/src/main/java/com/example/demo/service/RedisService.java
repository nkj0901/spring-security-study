package com.example.demo.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/*
* 참고 https://green-bin.tistory.com/69?category=1116728
* */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisService {
    private final RedisTemplate<String, Object> redisTemplate;

    public void setValues(String key, String data) {
        ValueOperations<String, Object> values = redisTemplate.opsForValue();
        values.set(key, data);
    }

    public void setValues(String key, String data, Duration duration) {
        ValueOperations<String, Object> values = redisTemplate.opsForValue();
        values.set(key, data, duration);
        log.info("redis에 value 저장 완료");
    }

    @Transactional
    public String getValues(String key) {
        ValueOperations<String, Object> values = redisTemplate.opsForValue();
        if(values.get(key) == null) return "false";
        return (String) values.get(key);
    }

    public void deleteValues(String key) {
        redisTemplate.delete(key);
    }

    public void expireValues(String key, int timeout) {
        redisTemplate.expire(key, timeout, TimeUnit.MILLISECONDS);
    }

    public boolean checkExistsValue(String value) {
        return !value.equals("false");
    }
}
