package Redis.test.service;

import Redis.test.entity.weather;
import Redis.test.repository.weatherRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class weatherService {

    private static final long CACHE_TTL_MINUTES = 15;

    @Autowired
    private weatherRepository weatherRepo;

    @Autowired
    private redisService redisService;

    /**
     * Cache-aside pattern:
     *   1. Check Redis for "weather:{cityName}"
     *   2. HIT  → return cached weather object
     *   3. MISS → query MySQL via repository
     *            → store result in Redis with TTL
     *            → return weather object
     */
    public weather getWeather(String cityName) {
        String cacheKey = "weather:" + cityName;

        // 1. Try Redis first
        weather cached = redisService.get(cacheKey, weather.class);
        if (cached != null) {
            log.info("CACHE HIT — serving '{}' from Redis", cityName);
            return cached;
        }

        // 2. Cache miss — query MySQL
        log.info("CACHE MISS — querying MySQL for '{}'", cityName);
        weather fromDb = weatherRepo.findByCityName(cityName);

        // 3. Store in Redis for next time (only if found in DB)
        if (fromDb != null) {
            redisService.set(cacheKey, fromDb, CACHE_TTL_MINUTES);
        }

        return fromDb;
    }
}
