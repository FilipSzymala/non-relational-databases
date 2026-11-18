package pl.nbd.infrastructure.redis;

import org.junit.jupiter.api.Test;
import pl.nbd.infrastructure.util.RedisClientFactory;
import redis.clients.jedis.JedisPooled;

import static org.junit.jupiter.api.Assertions.*;

class RedisClientFactoryTest {
    @Test
    void createsClientWithoutInstanceOrNetworkRequest() {
        try (JedisPooled redis = RedisClientFactory.create()) {
            assertNotNull(redis);
            assertTrue(RedisClientFactory.ttlSeconds() > 0);
        }
    }
}
