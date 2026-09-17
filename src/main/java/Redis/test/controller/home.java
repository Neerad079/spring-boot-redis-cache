package Redis.test.controller;

import Redis.test.service.redisTests;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class home {
    @Autowired
    private redisTests redisTests;

    @GetMapping("/")
    public String base(){
        redisTests.testmail();
        return "its Done bro";
    }
}
