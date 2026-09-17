package Redis.test.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;
@Service
public class redisTests {
    @Autowired // injects the RedisTemplate bean from redisConfig.java
    private RedisTemplate redisTemplate;

    public void testmail(){
//        redisTemplate.opsForValue().set("email", "079@gmail.com");
         Object salary=redisTemplate.opsForValue().get("salary");
         int a=1;
    }
}
