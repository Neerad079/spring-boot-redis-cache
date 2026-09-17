package Redis.test.repository;

import Redis.test.entity.weather;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface weatherRepository extends JpaRepository<weather, Long> {
    weather findByCityName(String cityName);
}
