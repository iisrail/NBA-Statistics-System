package com.nba.stats.repository;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import com.nba.stats.entity.LiveStat;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Repository
@RequiredArgsConstructor
@Slf4j
public class LiveStatLineRepository {

    private final RedisTemplate<String, Object> redisTemplate;

    public void saveLiveStat(LiveStat stat) {
        String key = stat.getRedisKey();

        redisTemplate.opsForValue().set(key, stat);
    }
}
