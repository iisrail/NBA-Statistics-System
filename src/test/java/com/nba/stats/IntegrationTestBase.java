package com.nba.stats;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import redis.embedded.RedisServer;

import java.io.IOException;

@SpringBootTest
public abstract class IntegrationTestBase {

    private static RedisServer redisServer;

    @BeforeAll
    static void startRedis() throws IOException {
        redisServer = new RedisServer(6370); // Different port for tests
        redisServer.start();
    }

    @AfterAll
    static void stopRedis() throws IOException {
        if (redisServer != null) {
            redisServer.stop();
        }
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.redis.host", () -> "localhost");
        registry.add("spring.redis.port", () -> 6370);
        registry.add("nba.current-season", () -> "2024/25");
    }
}