package Redis.test.controller;

import Redis.test.entity.weather;
import Redis.test.service.weatherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class weatherController {

    @Autowired
    private weatherService weatherService;

    @GetMapping("/weather")
    public weather showWeather(@RequestParam String cityName) {
        return weatherService.getWeather(cityName);
    }
}
