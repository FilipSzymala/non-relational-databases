package pl.nbd.infrastructure.util;

import redis.clients.jedis.JedisPooled;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.util.Objects;
import java.util.Properties;

public final class RedisClientFactory {
    private static final Properties properties = new Properties();

    public RedisClientFactory() {
        try (var input = Objects.requireNonNull(
            RedisClientFactory.class.getResourceAsStream("/redis.properties"), "Missing redis.properties")) {
            properties.load(input);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    public static JedisPooled create() {
        return new JedisPooled(
            URI.create(properties.getProperty("redis.uri")),
            Integer.parseInt(properties.getProperty("redis.timeoutMillis")));
    }

    public long ttlSeconds() {
        return Long.parseLong(properties.getProperty("redis.ttlSeconds"));
    }
}
