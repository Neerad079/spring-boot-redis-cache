# 🔴 Spring Boot Redis Cache

A reference project demonstrating how to integrate **Redis** as a caching layer in a **Spring Boot** application using the **Cache-Aside (Lazy Loading)** pattern with **MySQL** as the primary database.

> **Use case:** Multiple users request weather data for the same city. Instead of hitting the database every time, the first request fetches from MySQL and caches the result in Redis. All subsequent requests are served directly from Redis until the cache expires.

---

## 📐 Architecture

```
Client Request
    │
    ▼
┌──────────────────────┐
│  weatherController   │   GET /weather?cityName=Mumbai
│  (@RestController)   │
└──────────┬───────────┘
           │
           ▼
┌──────────────────────┐
│   weatherService     │   Cache-aside logic lives here
│   (@Service)         │
└──────┬───────┬───────┘
       │       │
  ┌────▼──┐ ┌──▼──────────┐
  │ Redis │ │    MySQL     │
  │ Cache │ │   Database   │
  └───────┘ └──────────────┘
```

### Cache-Aside Flow

```
1. Check Redis for key "weather:{cityName}"
2. HIT  → return cached object instantly (no DB query)
3. MISS → query MySQL → cache the result in Redis with a TTL → return
```

---

## 🛠 Tech Stack

| Component        | Technology                              |
|------------------|-----------------------------------------|
| Framework        | Spring Boot 4.1.1                       |
| Language         | Java 21                                 |
| Cache            | Redis (via `spring-boot-starter-data-redis`) |
| Database         | MySQL (via `mysql-connector-j`)         |
| ORM              | Hibernate / Spring Data JPA             |
| Build Tool       | Maven                                   |
| Utilities        | Lombok                                  |

---

## 📂 Project Structure

```
src/main/java/Redis/test/
├── TestApplication.java              # Spring Boot entry point
├── config/
│   └── redisConfig.java              # RedisTemplate bean configuration
├── controller/
│   └── weatherController.java        # REST endpoint: GET /weather
├── entity/
│   └── weather.java                  # JPA entity (id, cityName, temperature)
├── repository/
│   └── weatherRepository.java        # Spring Data JPA repository
└── service/
    ├── redisService.java             # Generic Redis get/set with JSON serialization
    └── weatherService.java           # Cache-aside logic (Redis → MySQL fallback)
```

---

## ⚙️ Prerequisites

Before running this project, make sure you have:

- **Java 21** installed
- **Maven** installed (or use the included `mvnw` wrapper)
- **MySQL** running on `localhost:3306`
- **Redis** running on `localhost:6379`

---

## 🚀 Setup Guide

### 1. Clone the repo

```bash
git clone https://github.com/<your-username>/spring-boot-redis-cache.git
cd spring-boot-redis-cache
```

### 2. Create the MySQL database

```sql
CREATE DATABASE IF NOT EXISTS weatherdb;
USE weatherdb;

-- The table is auto-created by Hibernate (ddl-auto: update),
-- but if you want to create it manually:
CREATE TABLE weather (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    city_name VARCHAR(255),
    temperature VARCHAR(255)
);

-- Insert sample data
INSERT INTO weather (city_name, temperature) VALUES ('Mumbai', '32°C');
INSERT INTO weather (city_name, temperature) VALUES ('Delhi', '40°C');
INSERT INTO weather (city_name, temperature) VALUES ('Bangalore', '28°C');
INSERT INTO weather (city_name, temperature) VALUES ('Chennai', '35°C');
INSERT INTO weather (city_name, temperature) VALUES ('Kolkata', '33°C');
```

### 3. Configure the application

Edit `src/main/resources/application.yaml` with your MySQL and Redis credentials:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/weatherdb
    username: your_mysql_username
    password: your_mysql_password
    driver-class-name: com.mysql.cj.jdbc.Driver

  jpa:
    hibernate:
      ddl-auto: update       # auto-creates/updates the table
    show-sql: true            # prints SQL queries in logs

  data:
    redis:
      host: localhost
      port: 6379
```

### 4. Install Redis (if you haven't)

**Windows (via WSL or Memurai):**
```bash
# Using WSL (Ubuntu):
sudo apt update && sudo apt install redis-server
sudo service redis-server start

# Verify:
redis-cli ping    # Should return PONG
```

**macOS:**
```bash
brew install redis
brew services start redis
```

**Linux:**
```bash
sudo apt install redis-server
sudo systemctl start redis
```

### 5. Run the application

```bash
# Using Maven wrapper (recommended)
./mvnw spring-boot:run

# Or using Maven directly
mvn spring-boot:run
```

The server starts on `http://localhost:8080`.

---

## 📡 API Endpoint

### `GET /weather?cityName={city}`

Fetches weather data for a city. Uses Redis cache if available, otherwise queries MySQL.

**Example:**
```bash
curl http://localhost:8080/weather?cityName=Mumbai
```

**Response:**
```json
{
  "id": 1,
  "cityName": "Mumbai",
  "temperature": "32°C"
}
```

---

## 🔍 How to Verify Caching is Working

Watch the application logs after making requests:

```bash
# First request — CACHE MISS (queries MySQL, caches in Redis)
curl http://localhost:8080/weather?cityName=Mumbai
```
```
INFO  — CACHE MISS — querying MySQL for 'Mumbai'
Hibernate: select ... from weather where city_name=?
INFO  — Cached key 'weather:Mumbai' in Redis with TTL=15 min
```

```bash
# Second request — CACHE HIT (served from Redis, no SQL query)
curl http://localhost:8080/weather?cityName=Mumbai
```
```
INFO  — CACHE HIT — serving 'Mumbai' from Redis
```

> Notice: No `Hibernate: select ...` line on the second call — that's Redis doing its job.

You can also inspect Redis directly:

```bash
redis-cli
> GET "weather:Mumbai"
# Returns the cached JSON string
> TTL "weather:Mumbai"
# Returns remaining seconds before expiry
```

---

## 🧠 Key Concepts Explained

### What is Cache-Aside (Lazy Loading)?

The application **doesn't** pre-load the cache. Instead:
- On a **cache miss**, it fetches from the database and stores the result in Redis
- On a **cache hit**, it returns the cached value directly
- Cached entries **expire** after a configurable TTL (default: 15 minutes)

This is the most common caching strategy because it's simple, safe, and only caches data that's actually requested.

### How RedisTemplate Works

`RedisTemplate` is Spring's abstraction for Redis operations. In this project:

```java
// Configuration (redisConfig.java)
RedisTemplate redisTemplate = new RedisTemplate<>();
redisTemplate.setConnectionFactory(factory);
redisTemplate.setKeySerializer(new StringRedisSerializer());     // keys stored as strings
redisTemplate.setValueSerializer(new StringRedisSerializer());   // values stored as strings
```

Since both key and value are serialized as plain strings, we manually convert Java objects to/from JSON using Jackson's `ObjectMapper` in `redisService.java`.

### How redisService Works

A generic, reusable service with two methods:

| Method | What it does |
|--------|-------------|
| `get(key, Class<T>)` | Fetches value from Redis → deserializes JSON → returns typed Java object |
| `set(key, value, ttlMinutes)` | Serializes Java object → stores as JSON string in Redis with TTL |

These methods are **entity-agnostic** — you can reuse them for any entity, not just weather.

---

## 📁 How to Add Redis Caching to Your Own Entity

Want to cache a different entity (e.g., `Product`)? Here's the pattern:

### 1. Create your entity
```java
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private double price;
}
```

### 2. Create a repository
```java
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    Product findByName(String name);
}
```

### 3. Add cache-aside logic in your service
```java
@Service
public class ProductService {

    @Autowired private ProductRepository productRepo;
    @Autowired private redisService redisService;

    public Product getProduct(String name) {
        String cacheKey = "product:" + name;

        // 1. Try Redis
        Product cached = redisService.get(cacheKey, Product.class);
        if (cached != null) return cached;

        // 2. Fallback to DB
        Product fromDb = productRepo.findByName(name);

        // 3. Cache for next time
        if (fromDb != null) {
            redisService.set(cacheKey, fromDb, 30); // 30 min TTL
        }
        return fromDb;
    }
}
```

That's it. The `redisService` handles all the serialization — you just pick a key pattern and a TTL.

---

## ⚠️ Common Issues

| Problem | Solution |
|---------|----------|
| `Cannot connect to Redis` | Make sure Redis is running: `redis-cli ping` should return `PONG` |
| `Communications link failure` (MySQL) | Check MySQL is running and the database `weatherdb` exists |
| `Table 'weather' doesn't exist` | Set `ddl-auto: update` in `application.yaml` — Hibernate creates it automatically |
| Cached data is stale | Reduce the TTL value or manually flush: `redis-cli FLUSHDB` |
| `NullPointerException` in redisService | The key doesn't exist in Redis — this is handled gracefully (returns `null`) |

---

## 📜 License

This project is open-source and available under the [MIT License](LICENSE).
