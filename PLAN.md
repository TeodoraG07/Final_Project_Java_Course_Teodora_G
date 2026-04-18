# Implementation Plan — Book Explorer (Java Final Project)

## Context
Greenfield Spring Boot application that streams book data from the Open Library API through Kafka into MongoDB, then serves it via Thymeleaf UI and a REST API. The project follows a strict 3-layer architecture (Repository → Service → Controller) and must hit 80 % test coverage on the service layer.

Nice-to-haves in scope: Docker Compose + Dockerfile.
Nice-to-haves out of scope: AppUser entity / auth.

---

## Files to Create (key paths)

```
pom.xml
src/main/java/com/example/bookexplorer/
  model/          Book.java  Author.java  Subject.java  KafkaEvent.java  SearchLog.java
  repository/     BookRepository.java  AuthorRepository.java  SubjectRepository.java
                  KafkaEventRepository.java  SearchLogRepository.java
  kafka/          BookMessage.java  BookProducerJob.java  BookConsumer.java
                  KafkaConfig.java
  client/         OpenLibraryClient.java
  service/        BookService.java  BookServiceImpl.java
  controller/     ExplorerController.java  BookRestController.java
  exception/      GlobalExceptionHandler.java  ResourceNotFoundException.java
src/main/resources/
  application.yml
  templates/      index.html  subject.html  error.html
  static/css/     style.css
docker-compose.yml
Dockerfile
src/test/java/com/example/bookexplorer/
  service/        BookServiceImplTest.java
  controller/     ExplorerControllerTest.java
  client/         OpenLibraryClientTest.java
```

---

## Sequential Tasks

### TASK 1 — Maven project bootstrap
**Goal:** Create `pom.xml` with all required Spring Boot starters and plugins.

Dependencies to include:
- `spring-boot-starter-web`
- `spring-boot-starter-data-mongodb`
- `spring-boot-starter-thymeleaf`
- `spring-kafka`
- `spring-boot-starter-quartz`
- `spring-boot-starter-validation`
- `spring-boot-starter-test` (scope: test) + `mockito-core`
- `jacoco-maven-plugin` (80 % line/branch minimum on `service` package)

Also create the base package `com.example.bookexplorer` with `BookExplorerApplication.java`.

---

### TASK 2 — MongoDB document model
**Goal:** Create the 5 document classes in `model/`.

| Class | Key annotations | Notable fields |
|---|---|---|
| `Author` | `@Document("authors")` | `id`, `olAuthorKey` (unique), `name` |
| `Subject` | `@Document("subjects")` | `id`, `name` (unique), `bookCount` |
| `Book` | `@Document("books")` | `id`, `olKey` (unique), `title`, `firstPublishYear`, `editionCount`, `coverId`, `@DBRef List<Author>`, `@DBRef List<Subject>` |
| `KafkaEvent` | `@Document("kafka_events")` | `id`, `@DBRef Book bookId`, `topic`, `offset`, `status`, `consumedAt` |
| `SearchLog` | `@Document("search_logs")` | `id`, `query`, `resultsCount`, `searchedAt` |

Validation: `@NotBlank` on all string keys/names; `@Min(1000)` on `firstPublishYear`.

---

### TASK 3 — Repository layer
**Goal:** Create 5 interfaces in `repository/`, all extending `MongoRepository`.

Extra query methods needed:
- `BookRepository`: `findBySubjectsContaining(Subject s)`, `findByOlKey(String key)`
- `AuthorRepository`: `findByOlAuthorKey(String key)`
- `SubjectRepository`: `findByName(String name)`
- `KafkaEventRepository`: `findByBookId(Book book)`
- `SearchLogRepository`: (none beyond CRUD)

---

### TASK 4 — Kafka configuration
**Goal:** Create `KafkaConfig.java` that declares the `biblio-stream` topic (1 partition, 1 replica) as a `NewTopic` bean, plus `ProducerFactory` and `ConsumerFactory` beans wired from `application.yml`.

`application.yml` Kafka section:
```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
    consumer:
      group-id: book-explorer-group
      auto-offset-reset: earliest
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      properties:
        spring.json.trusted.packages: "com.example.bookexplorer.kafka"
```

---

### TASK 5 — BookMessage POJO
**Goal:** Create `kafka/BookMessage.java` — a plain serialisable POJO (no `@Document`).

Fields: `olKey`, `title`, `authorName`, `authorKey`, `subject`, `firstPublishYear`, `editionCount`, `coverId`.
Add a no-arg constructor + all-args constructor for Jackson deserialization.

---

### TASK 6 — Open Library client + Quartz producer job
**Goal:** Implement two classes:

**`OpenLibraryClient`**
- Injects `RestTemplate`.
- Method `List<BookMessage> fetchBySubject(String subject)` — calls `https://openlibrary.org/search.json?subject={subject}&limit=20`.
- Parses the JSON response (fields: `key`, `title`, `author_name`, `author_key`, `first_publish_year`, `edition_count`, `cover_i`).
- Returns an empty list on HTTP error (log at ERROR).

**`BookProducerJob`** (`@DisallowConcurrentExecution`)
- Implements Quartz `Job`.
- Configured subjects list injected from `application.yml` (`app.subjects`).
- Every 2 minutes: for each subject → call client → publish each `BookMessage` to `biblio-stream` via `KafkaTemplate`.
- Log at INFO per batch, DEBUG per message.

Quartz schedule in `application.yml`:
```yaml
spring.quartz:
  job-store-type: memory
  properties:
    org.quartz.threadPool.threadCount: 1
app:
  subjects:
    - fantasy
    - science
    - history
```

---

### TASK 7 — Kafka consumer
**Goal:** Create `BookConsumer.java` with a `@KafkaListener(topics = "biblio-stream")` method.

For each `BookMessage` received:
1. Delegate to `BookServiceImpl.upsertBook(message)`.
2. Save a `KafkaEvent` with status `CONSUMED`, `consumedAt = Instant.now()`, and the message's Kafka `offset` (injected via `@Header(KafkaHeaders.OFFSET)`).
3. Log at INFO.
4. On any exception: log at ERROR, save `KafkaEvent` with status `FAILED`.

---

### TASK 8 — Service layer
**Goal:** Create `BookService` interface and `BookServiceImpl`.

Interface methods:
```java
void upsertBook(BookMessage msg);
List<Book> getBySubject(String subjectName);
List<AuthorCount> getTopAuthors(String subjectName, int limit);   // AuthorCount: record { Author author; long count; }
Map<String, Long> getSubjectBookCounts();
StatsDto getStats();   // record { long totalBooks, totalSubjects, totalAuthors, totalMessages; }
```

`upsertBook` logic:
1. Find-or-create `Author` by `olAuthorKey`.
2. Find-or-create `Subject` by `name`; increment `bookCount` if new book.
3. Find-or-create `Book` by `olKey`; update fields if exists.
4. Save all three.

All methods: log INFO on entry, DEBUG for detail, ERROR on catch.

---

### TASK 9 — Controller layer

**`ExplorerController`** (`@Controller`):
- `GET /` → resolves `index.html`; adds `stats`, `subjects` to model.
- `GET /subject?name=X` → resolves `subject.html`; saves `SearchLog`; adds `books`, `topAuthors`, `subjectName` to model.

**`BookRestController`** (`@RestController`, base path `/api`):
- `GET /api/subjects` → `Map<String, Long>` from `getSubjectBookCounts()`
- `GET /api/books?subject=X` → `List<Book>` or 404 via `ResourceNotFoundException`
- `GET /api/authors?subject=X` → `List<AuthorCount>`
- `GET /api/stats` → `StatsDto`

**`GlobalExceptionHandler`** (`@ControllerAdvice`):
- `ResourceNotFoundException` → 404
- `MethodArgumentNotValidException` → 400
- `Exception` → 500

---

### TASK 10 — Thymeleaf UI

**`index.html`** (Home):
- 4 stat cards: books indexed, subjects, authors, messages consumed.
- Subject bubbles — each is an `<a>` to `/subject?name={name}`.

**`subject.html`** (Subject page):
- Book cards: cover image (`https://covers.openlibrary.org/b/id/{coverId}-M.jpg`), title, author, year.
- Top-authors leaderboard: horizontal bar chart using inline CSS width proportional to work count.

**`error.html`**: generic error page used by Spring Boot's error controller.

Add minimal `style.css` for card grid and bar chart layout.

---

### TASK 11 — Unit tests

**`BookServiceImplTest`** (Mockito, JUnit 5):
- Mock `BookRepository`, `AuthorRepository`, `SubjectRepository`.
- Test: `upsertBook` saves new book, `upsertBook` deduplicates by `olKey`, `getBySubject` returns correct list, `getTopAuthors` ranks correctly.

**`ExplorerControllerTest`** (`@WebMvcTest`):
- Mock `BookService`.
- Test: correct template name returned, model attributes populated for `/` and `/subject`.

**`OpenLibraryClientTest`** (Mockito):
- Mock `RestTemplate`.
- Test: JSON parsed correctly into `BookMessage` list, error HTTP response returns empty list.

---

### TASK 12 — Docker Compose + Dockerfile

**`docker-compose.yml`**:
```yaml
services:
  zookeeper:   confluentinc/cp-zookeeper:7.5.0   port 2181
  kafka:       confluentinc/cp-kafka:7.5.0        port 9092, depends_on zookeeper
  mongodb:     mongo:7.0                           port 27017, volume mongo_data
```

**`Dockerfile`** (multi-stage):
- Stage 1 `maven:3.9-eclipse-temurin-21`: `mvn package -DskipTests`
- Stage 2 `eclipse-temurin:21-jre-alpine`: copy JAR, `ENTRYPOINT java -jar`

---

## Verification

1. `mvn clean verify` — all tests pass, JaCoCo report shows ≥ 80 % on service package.
2. `docker compose up -d` — Zookeeper, Kafka, MongoDB healthy.
3. Run app (`mvn spring-boot:run`) — Quartz fires every 2 min, logs show messages consumed.
4. Open `http://localhost:8080` — stat cards show counts, subject bubbles visible.
5. Click a subject bubble — book cards appear, bar chart renders, SearchLog saved in MongoDB.
6. `curl http://localhost:8080/api/stats` — returns JSON with all four counts.
7. `curl http://localhost:8080/api/books?subject=nonexistent` — returns 404.
