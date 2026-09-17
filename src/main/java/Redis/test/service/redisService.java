package Redis.test.service;

import tools.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class redisService {

    @Autowired
    private RedisTemplate redisTemplate;

    // GET — deserialize JSON string from Redis into a Java object
    public <T> T get(String key, Class<T> entityClass) {
        try {
            Object o = redisTemplate.opsForValue().get(key);
            if (o == null) {
                return null;  // cache miss
            }
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(o.toString(), entityClass);
        } catch (Exception e) {
            log.error("Exception while getting key '{}' from Redis", key, e);
            return null;
        }
    }

    // SET — serialize Java object to JSON and store in Redis with a TTL
    public void set(String key, Object value, long ttlMinutes) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            String json = mapper.writeValueAsString(value);
            redisTemplate.opsForValue().set(key, json, ttlMinutes, TimeUnit.MINUTES);
            log.info("Cached key '{}' in Redis with TTL={} min", key, ttlMinutes);
        } catch (Exception e) {
            log.error("Exception while setting key '{}' in Redis", key, e);
        }
    }
}
