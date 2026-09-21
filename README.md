# ForgeIT

ForgeIT helps integration tests spin up reusable infrastructure features while keeping
consumer code focused on HTTP contract definitions. The core `ForgeIT` interface is
annotation-driven and bundles WireMock support so that test suites can stub and verify
external HTTP integrations with minimal boilerplate.

## Installation

Add the bundle and annotation processor to your test-scoped dependencies. The bundle
aggregates all ForgeIT modules, including the WireMock feature implementation.

```xml
<dependency>
    <groupId>com.sitionix.forgeit</groupId>
    <artifactId>forgeit</artifactId>
    <version>0.0.7-SNAPSHOT</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>com.sitionix.forgeit</groupId>
    <artifactId>forge-it-annotation-processor</artifactId>
    <version>0.0.7-SNAPSHOT</version>
    <scope>test</scope>
</dependency>
```

Create your own entry-point interface that extends `ForgeIT` and declares the features
you want via `@ForgeFeatures`. WireMock is already part of the public contract, but you
can declare it explicitly for clarity:

```java
@ForgeFeatures(WireMockSupport.class)
public interface ConsumerTests extends ForgeIT {
}
```

## Fixture root

All file-based fixtures and mappings resolve under the unchangeable
`src/test/resources/forge-it` root. Module-specific paths (for example,
`forge-it.modules.wiremock.mapping.request`) can be customized, but the `forge-it` root
folder itself is fixed and cannot be overridden.

## WireMock support

### Entry point

Declare the feature on your test interface so ForgeIT installs WireMock support:

```java
@ForgeFeatures(WireMockSupport.class)
public interface ConsumerWireMockTests extends ForgeIT {
}
```

### Configuration

WireMock starts automatically through Testcontainers by default. You can override the
behaviour with Spring configuration properties:

```yaml
forge-it:
  modules:
    wiremock:
      enabled: true           # disable to skip WireMock entirely
      mode: internal          # or "external" to reuse an existing instance
      host: localhost         # required when mode is external
      port: 8089              # required when mode is external
      mapping:
        request: /wiremock/request
        response: /wiremock/response
        default-request: /wiremock/default/request
        default-response: /wiremock/default/response
```

When `mode` is `internal`, the module launches a `wiremock/wiremock:3.6.0` container and
publishes the base URL as `forge-it.wiremock.base-url`, `forge-it.wiremock.port`, and
`forge-it.wiremock.host` environment properties.

### Creating stubs

`WireMockSupport` exposes a `wiremock()` helper that returns a `WireMockJournal` for
building stubs and assertions. A common pattern is to prepare a mapping, exercise the
system under test, and then verify the invocation:

```java
final RequestBuilder<?, ?> requestBuilder = forgeit.wiremock().createMapping(AuthEndpoints.login())
        .matchesJson("requestLoginUserWithHappyPath.json")
        .responseBody("responseLoginUserWithHappyPath.json")
        .responseStatus(HttpStatus.OK)
        .plainUrl()
        .create();

// Call the controller under test
mockMvc.perform(post("/auth/login")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(new LoginRequest("john.doe", "s3cr3t"))))
    .andExpect(status().isOk());

requestBuilder.verify();
```

Key builder options:

- `plainUrl()`, `urlWithQueryParam(...)`, `path(...)`, and `pathPattern(...)` choose how
the stub matches the endpoint URL, including path and query parameter templating.
- `matchesJson(...)` and `responseBody(...)` load request/response bodies from your
resource folders (mutators allow you to tweak payloads programmatically).
- `responseStatus(...)` and `delayForResponse(...)` configure status codes and latency.
- `applyDefault(...)` and `createDefault(...)` apply reusable defaults across multiple
stubs.

#### Fixture locations for WireMock JSON
Request and response payloads resolve relative to the configured `mapping.request` and
`mapping.response` paths under the unchangeable `src/test/resources/forge-it` root
(defaults: `/wiremock/request` and `/wiremock/response`). Default payloads consumed by
`applyDefault(...)` and `createDefault(...)` load from `mapping.default-request` and
`mapping.default-response`.
Keep reusable templates in the default folders (for example,
`forge-it/wiremock/default/response/responseLoginDefault.json`) and store per-scenario
payloads under the main request/response paths. Mutators work with either location so you
can adjust fixtures at runtime without editing files.

#### Default logic and reusable stubs

Default mappings let you define a reusable baseline and selectively override it per
stub. The default loader points at the `default-request` and `default-response` resource
locations configured under `forge-it.modules.wiremock.mapping`.

```java
// Define a default stub for the login endpoint
forgeit.wiremock().createMapping(AuthEndpoints.login())
        .applyDefault(defaults -> defaults
                .matchesJson("requestLoginDefault.json")
                .responseBody("responseLoginDefault.json")
                .responseStatus(HttpStatus.OK)
                .plainUrl())
        .createDefault();

// Override only the response body for an error scenario using the default mapping
forgeit.wiremock().createMapping(AuthEndpoints.login())
        .responseBody("responseLoginError.json")
        .responseStatus(HttpStatus.UNAUTHORIZED)
        .create();
```

`applyDefault(...)` sets the defaults once, while `createDefault(...)` applies them when
registering the mapping. If no overrides are provided, the default request/response
payloads and status are used as-is.

#### Request and response mutators

Mutators let you adjust JSON payloads before registration without editing fixture files.
You can set per-stub mutators directly, or define default mutators that run whenever a
default mapping is used.

```java
// Per-stub mutation of the request body loaded from the resource file
forgeit.wiremock().createMapping(AuthEndpoints.login())
        .matchesJson("requestLoginUser.json", req -> req.setPassword("overridden"))
        .responseBody("responseLoginUser.json")
        .create();

// Default request/response mutators applied to every default mapping
forgeit.wiremock().createMapping(AuthEndpoints.login())
        .createDefault(defaults -> defaults
                .mutateRequest(req -> req.setUsername("default-user"))
                .mutateResponse(res -> res.setToken("static-token"))
        );
```

When a default mapping is used (`createDefault(...)`), the mutator defined on that
invocation takes precedence; otherwise the previously configured default mutators are
applied. Per-stub mutators always override defaults for that specific mapping.

### Verifying calls

`create()` returns a `RequestBuilder` that can assert expectations about recorded
traffic:

- `verify()` checks that the endpoint was called the expected number of times (defaults
to once).
- `atLeastTimes(...)` enforces a specific call count.
- `jsonName(...)`/`json(...)` assert a request body, optionally ignoring fields with
`ignoringFields(...)`.
- `pathWithParameters(...)` can interpolate path placeholders before validation.

You can also reset WireMock between scenarios with `forgeit.wiremock().reset()` to clear
previous mappings and journal entries.

## MockMvc support

`MockMvcSupport` offers a fluent builder for invoking your controllers with request/response
fixtures while keeping assertions centralized in a single place.

### Entry point

Declare the feature on your test interface so ForgeIT installs MockMvc support:

```java
@ForgeFeatures(MockMvcSupport.class)
public interface ConsumerMockMvcTests extends ForgeIT {
}
```

### Configuration

The module ships with sensible defaults for loading request and response payloads from the
classpath. Override the subpaths if your project stores fixtures elsewhere under the
unchangeable `src/test/resources/forge-it` root:

```yaml
forge-it:
  modules:
    mock-mvc:
      default-token: Bearer static-token
      default-headers:
        x-tenant-id: tenant-01
        x-user-id: 42
      path:
        request: /mockmvc/request         # standard request fixture folder
        response: /mockmvc/response       # standard response fixture folder
        default-request: /mockmvc/default/request
        default-response: /mockmvc/default/response
```

Request and response JSON files always resolve under the unchangeable
`src/test/resources/forge-it` root, so `withRequest("loginRequest.json")` resolves to
`forge-it/mockmvc/request/loginRequest.json`.
Defaults declared on an `Endpoint` or via `executeDefault(...)` read from the
`default-request`/`default-response` folders, while per-test overrides sit under the main
`request`/`response` paths. This separation keeps reusable templates tidy without blocking
bespoke payloads for specific scenarios.

### Executing requests

Use `forgeit.mockMvc().ping(...)` with an `Endpoint` definition to drive the request.
Request/response bodies are loaded from the configured folders and can be mutated before
execution:

```java
forgeit.mockMvc()
        .ping(MockMvcEndpoint.login())
        .withRequest("loginRequest.json", req -> {
            req.setUsername("username");
            req.setPassword("password");
        })
        .expectResponse("loginResponse.json", res -> res.setToken("mutated-token"))
        .expectStatus(HttpStatus.OK)
        .execute();
```

Add `andExpectPath(...)` for extra matchers, `token(...)` to attach an `Authorization`
header, and `header(name, value)` for arbitrary headers. Response assertions can ignore fields
via `expectResponse(..., fieldsToIgnore...)`.

### Defaults and reusable fixtures

Endpoints can declare defaults (request, response, status, token, headers) that the builder
reuses via `executeDefault()`. You can further mutate the defaults at call time:

```java
// Endpoint with baked-in defaults
public static Endpoint<LoginRequest, LoginResponse> loginDefault() {
    return Endpoint.createContract(
            "/auth/login",
            HttpMethod.POST,
            LoginRequest.class,
            LoginResponse.class,
            (MockmvcDefault) ctx -> ctx
                    .withRequest("loginRequest.json")
                    .expectResponse("loginResponse.json")
                    .expectStatus(200)
                    .token("Bearer default-token")
                    .header("x-tenant-id", "tenant-01")
    );
}

// Invoke defaults and tweak fixtures per scenario
forgeit.mockMvc()
        .ping(MockMvcEndpoint.loginDefault())
        .executeDefault(d -> d
                .mutateRequest(r -> r.setPassword("password"))
                .mutateResponse(res -> res.setToken("mutated-token")));
```

When defaults exist, `applyDefault(...)` can override the default request/response names or
status before execution. If you skip request/response bodies altogether, the builder still
performs status-only assertions.

## PostgreSQL support

### Entry point

Declare the feature on your test interface so ForgeIT installs PostgreSQL support:

```java
@ForgeFeatures(PostgresqlSupport.class)
public interface ConsumerPostgresTests extends ForgeIT {
}
```

### Configuration
The PostgreSQL feature starts a `postgres:16-alpine` Testcontainers instance by default and
initialises schema/constraints/data from SQL under `/db/postgresql` (see the consumer
fixtures in `forge-it-consumer-it/src/test/resources/forge-it/db/postgresql/**`). Paths
are always resolved under the unchangeable `src/test/resources/forge-it` root. Override
paths or connection details via:

```yaml
forge-it:
  modules:
    postgresql:
      enabled: true             # set false to skip the module
      mode: internal            # or external to point at an existing DB
      connection:
        host: localhost
        port: 5432
        database: forge-it
        username: forge-it
        password: forge-it-pwd
      paths:
        ddl:
          path: /db/postgresql
        entity:
          defaults: /db/postgresql/entities/default
          custom: /db/postgresql/entities/custom
      tx-policy: REQUIRES_NEW    # REQUIRED | REQUIRES_NEW | MANDATORY
```

### Runtime wiring and failure modes
- Internal mode starts a Testcontainer and publishes runtime connection values under
  `forge-it.postgresql.connection.*` so the data source can bind to the container.
- External mode requires `forge-it.modules.postgresql.connection.host` and
  `forge-it.modules.postgresql.connection.port` (or an explicit `jdbc-url`); missing values
  fail fast during context startup.
- If `tx-policy` is `MANDATORY`, graph execution must run inside `@Transactional` or a
  `ForgeItConfigurationException` is raised.
- When `paths.ddl.path` is blank, schema initialization is skipped.

### Schema, constraints, and seed scripts
SQL scripts execute deterministically: everything under `schema/`, then `constraints/`,
then `data/`, and finally any other folder (treated as `custom`). Inside a phase,
filenames are ordered by their leading number, so `001_create_users.sql` runs before
`110_add_fk.sql`; files without a numeric prefix run last within the phase. Drop your own
DDL, constraint, and seed files under `src/test/resources/forge-it/db/postgresql/**` (or
the subpath set in `forge-it.modules.postgresql.paths.ddl.path`; the `forge-it` root is
fixed) to extend the database for your tests. The sample consumer illustrates the layout:

- `schema/*.sql` for table creation
- `constraints/*.sql` for foreign keys, unique indexes, or checks
- `data/*.sql` for reference or seed rows
- any other folder for custom scripts that should run after data loads

### Entity fixtures and JSON mapping
Entities can be hydrated from JSON rather than hand-built objects. Default bodies declared
via `.withDefaultBody(...)` load from `forge-it.modules.postgresql.paths.entity.defaults`
(defaults to `/db/postgresql/entities/default`), while `.withJson(...)` pulls from
`forge-it.modules.postgresql.paths.entity.custom` (default `/db/postgresql/entities/custom`).
Resources are resolved under the unchangeable `src/test/resources/forge-it` root, so
`withDefaultBody("default_user_entity.json")` maps to
`forge-it/db/postgresql/entities/default/default_user_entity.json`, and
`withJson("priority_user_entity.json")` maps to
`forge-it/db/postgresql/entities/custom/priority_user_entity.json`. Override the paths if
you prefer a different folder structure under the fixed `src/test/resources/forge-it` root.
Keep JSON aligned with the entity structure that Jackson deserialises; combine shared
defaults with scenario-specific custom files to avoid duplicating base shapes.

### Declaring contracts and building graphs
Use `DbContractsDsl` to describe entities, defaults, and dependencies. Cleanup defaults to
`DELETE_ALL` so the cleanup listener removes rows after each test; use `NONE` for lookup
tables you seed once.

```java
public static final DbContract<UserStatusEntity> STATUS =
        DbContractsDsl.entity(UserStatusEntity.class)
                .cleanupPolicy(CleanupPolicy.NONE)
                .build();

public static final DbContract<UserEntity> USER =
        DbContractsDsl.entity(UserEntity.class)
                .dependsOn(STATUS, UserEntity::setStatus)
                .withDefaultBody("default_user_entity.json")
                .cleanupPolicy(CleanupPolicy.DELETE_ALL)
                .build();
```

Create graphs that attach parents first, then dependents. Supply defaults, a custom JSON
fixture, or a fully constructed entity:

```java
DbGraphResult result = forgeit.postgresql()
        .create()
        .to(STATUS.getById(1L))                            // attach ACTIVE status
        .to(USER.withJson("custom_user_entity.json")       // or .withEntity(new UserEntity(...))
                .label("primary"))
        .build();

UserEntity created = result.entity(USER).get();
List<UserEntity> persisted = forgeit.postgresql().get(UserEntity.class).getAll();

result.entity(USER)
        .update(user -> user.setUsername("updated_user"));

result.entity(USER, "primary")
        .update(user -> user.setUsername("labeled_user"));

forgeit.postgresql()
        .create()
        .to(USER.withJson("custom_user_entity.json")
                .addChild(STATUS.getById(1L)))
        .build();

result.entities(USER)
        .forEach(user -> System.out.println(user.getUsername()));

result.entityAt(USER, 0)
        .update(user -> user.setUsername("first_user"));
```

### Cleanup, verification, and transactions
- `@IntegrationTest` registers a cleanup listener that executes `DELETE_ALL` contracts
  after each test; override phases with `cleanupPhase` or `@DbCleanup` when you need
  different timing.
- `CleanupPolicy.NONE` leaves reference data intact between tests; keep lookups (e.g.,
  statuses) on this policy and dependents on `DELETE_ALL`.
- Use `forgeit.postgresql().get(Entity.class)` to verify rows (`getAll()`, `getById(id)`) or
  fluent assertions (`hasSize(...)`, `singleElement()`, `andExpected(...)`,
  `allMatch()/anyMatch()/nonMatch()`) and `DbGraphResult` to assert on freshly persisted
  entities. When the same contract is invoked multiple times, `entity(contract)` returns
  the most recently stored one; use labels on the invocation to retrieve a specific
  instance. Use `dependsOnOptional(...)` for nullable relationships.
- Transaction boundaries for graph execution are controlled by `tx-policy`
  (`REQUIRES_NEW` by default). Choose `MANDATORY` if you want to reuse an outer
  `@Transactional` block.

### Entity assertions
`PostgresForge` exposes builders for comparing entities with JSON fixtures. The default
comparison checks only fields present in the fixture (extra entity fields are ignored),
while strict matching compares the entire JSON structure after removing ignored fields.
You can also match all entities for a contract without relying on call order.
Assertions always load fixtures from the custom entity path.

```java
forgeit.postgresql()
        .assertEntity(result.entityAt(PRODUCT, 0))
        .withJson("first_product_entity.json")
        .ignoreFields("id", "user")
        .assertMatches();

forgeit.postgresql()
        .assertEntity(result.entityAt(PRODUCT, 0))
        .withJson("first_product_entity.json")
        .ignoreFields("id", "user")
        .withDeepStructure()
        .assertMatchesStrict();

forgeit.postgresql()
        .assertEntities(PRODUCT)
        .containsAllWithJsons("first_product_entity.json", "second_product_entity.json");

forgeit.postgresql()
        .assertEntities(PRODUCT)
        .containsWithJsonsStrict("first_product_entity.json", "second_product_entity.json");

forgeit.postgresql()
        .assertEntities(PRODUCT)
        .hasSize(2)
        .containsAllWithJsons("first_product_entity.json", "second_product_entity.json");
```

`containsAllWithJsons(...)` asserts every fixture matches a distinct entity (extra entities
are allowed). `containsWithJsonsStrict(...)` enforces an exact count match and strict field matching.
Use `hasSize(...)` to assert the total count regardless of which matching method you call.

### Entity retrieval assertions
You can assert on retrieved entities with fluent predicates:

```java
UserEntity persisted = forgeit.postgresql()
        .get(UserEntity.class)
        .hasSize(1)
        .singleElement()
        .andExpected(user -> Objects.equals(user.getUsername(), "manual_user"))
        .assertEntity();

forgeit.postgresql()
        .get(UserEntity.class)
        .hasSize(3)
        .andExpected(user -> Objects.equals(user.getStatus().getDescription(), "ACTIVE"))
        .anyMatch();

forgeit.postgresql()
        .get(UserEntity.class)
        .andExpected(user -> Objects.equals(user.getStatus().getDescription(), "DELETED"))
        .nonMatch();
```

`singleElement()` asserts there is only one entity and returns a single-entity assertion chain.
After `singleElement()`, use `andExpected(...)` and `assertEntity()`; match operations are not available.
After `hasSize(...)`, you can keep chaining with `allMatch()`, `anyMatch()`, `nonMatch()` or
optionally `singleElement()` when the expected size is one.

### PostgreSQL test coverage
The sample integration tests exercise the critical flows and guard against common breakage:
- `forge-it-consumer-it/src/test/java/com/sitionix/forgeit/consumer/db/PostgresqlIT.java`
  covers graph creation, labels, ordering, JSON assertions, and fixtures.
- `forge-it-consumer-it/src/test/java/com/sitionix/forgeit/consumer/db/PostgresTransactionlessIT.java`
  verifies graphs can execute without an outer transaction (default policy).
- `forge-it-consumer-it/src/test/java/com/sitionix/forgeit/consumer/db/PostgresCleanupSmokeIT.java`
  ensures cleanup after each test.
- `forge-it-consumer-it/src/test/java/com/sitionix/forgeit/consumer/db/PostgresTxPolicyMandatoryIT.java`
  ensures `tx-policy=MANDATORY` fails without an active transaction.

## SQLite support

SQLite support mirrors the relational PostgreSQL API while using the Xerial SQLite JDBC
driver instead of a container. Declare the feature on your test interface:

```java
@ForgeFeatures(SqliteSupport.class)
public interface ConsumerSqliteTests extends ForgeIT {
}
```

By default the module creates a temporary file-backed SQLite database, publishes
`forge-it.sqlite.connection.*`, configures `spring.datasource.url`,
`spring.datasource.driver-class-name=org.sqlite.JDBC`, and sets Hibernate to
`org.hibernate.community.dialect.SQLiteDialect`. The `sqlite-jdbc` dependency carries the
native SQLite libraries for supported platforms.

```yaml
forge-it:
  modules:
    sqlite:
      enabled: true
      mode: internal            # or external for an existing SQLite file/URL
      connection:
        database: forge-it      # external mode path when jdbc-url is not set
        jdbc-url: jdbc:sqlite:/tmp/forge-it.db
      paths:
        ddl:
          path: /db/sqlite
        entity:
          defaults: /db/sqlite/entities/default
          custom: /db/sqlite/entities/custom
      tx-policy: REQUIRES_NEW
```

Use the same graph, retrieval, cleanup, and assertion patterns as PostgreSQL, replacing
`forgeit.postgresql()` with `forgeit.sqlite()`. SQLite SQL fixtures live under
`src/test/resources/forge-it/db/sqlite/**`; keep dialect-specific DDL there because
PostgreSQL constructs such as `BIGSERIAL` are not portable.

SQLite coverage:
- `forge-it-sqlite/src/test/java/com/sitionix/forgeit/sqlite/**` verifies defaults and
  connection wiring.
- `forge-it-consumer-it/src/test/java/com/sitionix/forgeit/consumer/sqlite/SqliteIT.java`
  mirrors the PostgreSQL relational graph, retrieval, JSON assertion, and constraint
  scenarios except for the Postgres-only MockMvc endpoint.
- `SqliteTransactionlessIT`, `SqliteCleanupSmokeIT`, and `SqliteTxPolicyMandatoryIT`
  mirror the PostgreSQL transaction and cleanup guard tests.

## MongoDB support

MongoDB support focuses on document scenarios and does not require `DbContract`
declarations. Documents are created directly via `mongo().create(EntityClass)`.

### Entry point

Declare the feature on your test interface so ForgeIT installs MongoDB support:

```java
@ForgeFeatures(MongoSupport.class)
public interface ConsumerMongoTests extends ForgeIT {
}
```

### Configuration

By default the module starts a `mongo:7.0` Testcontainer and wires
`spring.data.mongodb.uri` automatically. You can override behaviour with:

```yaml
forge-it:
  modules:
    mongodb:
      enabled: true
      mode: internal           # or external
      container:
        image: mongo:7.0
      connection:
        uri: mongodb://localhost:27017/forge-it
        host: localhost
        port: 27017
        database: forge-it
        uuid-representation: standard
      paths:
        entity:
          defaults: /db/mongodb/entities/default
          custom: /db/mongodb/entities/custom
          expected: /db/mongodb/entities/expected
```

Notes:
- Fixture paths are always resolved under the unchangeable
  `src/test/resources/forge-it` root.
- In `external` mode, provide `connection.uri` or `connection.host` +
  `connection.port`; otherwise startup fails fast.
- `connection.uuid-representation` supports:
  `standard`, `java_legacy`, `c_sharp_legacy`, `python_legacy`, `unspecified`
  (default: `standard`).

### Creating and mutating documents

Mongo documents are created without contracts:

```java
DbEntityHandle<SomeEntityClass> created = forgeit.mongo()
        .create(SomeEntityClass.class)
        .body("some_entity.json");

created.mutate(entity -> entity.setName("updated-name"));
```

You can also pass a ready object:

```java
forgeit.mongo()
        .create(SomeEntityClass.class)
        .body(SomeEntityClass.builder()
                .name("manual")
                .description("from-object")
                .build());
```

For JSON loading, ForgeIT checks `paths.entity.custom` first and then falls back
to `paths.entity.defaults`.

### Retrieval and assertions

Use retrievers for field-level checks:

```java
forgeit.mongo()
        .get(SomeEntityClass.class)
        .where(SomeEntityClass::getName, "updated-name")
        .hasSize(1)
        .singleElement()
        .andExpected(entity -> Objects.equals(entity.getDescription(), "seed description"))
        .assertEntity();
```

Use shared assertion builders for JSON-based comparisons:

```java
forgeit.mongo()
        .assertEntity(created)
        .withJson("some_entity.json")
        .ignoreFields("id")
        .assertMatchesStrict();

forgeit.mongo()
        .assertEntities(SomeEntityClass.class)
        .ignoreFields("id")
        .hasSize(1)
        .containsWithJsonsStrict("some_entity.json");
```

`assertEntity(...).withJson(...)` and `assertEntities(...).contains*WithJsons(...)`
load expected fixtures from `paths.entity.expected` (fallback: `paths.entity.custom`).

### Cleanup behaviour

Mongo cleanup is integrated into the same `@IntegrationTest` lifecycle as other
supports. By default (`cleanupPhase=AFTER_EACH`), ForgeIT drops Mongo data after
every test method, so scenarios stay isolated without manual reset calls.

### MongoDB test coverage

- `forge-it-consumer-it/src/test/java/com/sitionix/forgeit/consumer/mongo/MongoIT.java`
  covers JSON/object creation, mutation, retrieval, and assertion builders.
- `forge-it-consumer-it/src/test/java/com/sitionix/forgeit/consumer/mongo/MongoCleanupSmokeIT.java`
  verifies cleanup between ordered test methods.
- `forge-it-consumer-it/src/test/java/com/sitionix/forgeit/consumer/mongo/MongoMockMvcIT.java`
  verifies MockMvc -> Mongo persistence.
- `forge-it-consumer-it/src/test/java/com/sitionix/forgeit/consumer/MixedPersistenceIT.java`
  verifies Mongo and PostgreSQL persistence/verification in one IT.

## Kafka support

Kafka support provides publish/consume helpers that load JSON fixtures and run against
either a Testcontainers Kafka broker or an external cluster.

### Entry point

Declare the feature on your test interface so ForgeIT installs Kafka support:

```java
@ForgeFeatures(KafkaSupport.class)
public interface ConsumerKafkaTests extends ForgeIT {
}
```

### Configuration

The module ships with defaults that start an internal broker and configure Spring Kafka
for String payloads. Override only what you need:

```yaml
forge-it:
  modules:
    kafka:
      enabled: true
      mode: internal
      bootstrap-servers: localhost:9092
      consumer:
        poll-timeout-ms: 5000
        auto-offset-reset: earliest
      path:
        payload: /kafka/payload
        expected: /kafka/expected
        metadata: /kafka/metadata
        default-payload: /kafka/default/payload
        default-expected: /kafka/default/expected
        default-metadata: /kafka/default/metadata
      container:
        image: confluentinc/cp-kafka:7.6.1
```

Notes:
- `bootstrap-servers` is injected into `spring.kafka.bootstrap-servers` automatically.
- Default serializers/deserializers are set to String unless you override them.
- `consumer.group-id` is optional; you can supply the group id per consumer contract
  via `.groupId(...)` instead of adding application YAML.

### Contracts and fixtures

Build producer and consumer contracts that point at topics and fixture defaults. If you
use envelopes, the contract type is the envelope type:

```java
public static final KafkaContract<UserCreatedEnvelope> USER_CREATED_INPUT =
        KafkaContract.producerContract()
                .topicFromProperty("consumer.kafka.input-topic")
                .defaultEnvelope(UserCreatedEnvelope.class)
                .defaultPayload(UserCreatedEvent.class, "defaultUserCreatedEvent.json")
                .defaultMetadata(UserCreatedMetadata.class, "defaultUserCreatedMetadata.json")
                .build();

public static final KafkaContract<UserCreatedEnvelope> USER_CREATED_OUTPUT =
        KafkaContract.consumerContract()
                .topicFromProperty("consumer.kafka.output-topic")
                .groupId("forge-it-consumer")
                .defaultEnvelope(UserCreatedEnvelope.class)
                .defaultExpectedPayload(UserCreatedEvent.class, "defaultUserCreatedEvent.json")
                .defaultMetadata(UserCreatedMetadata.class, "defaultUserCreatedMetadata.json")
                .build();
```

Fixture resolution mirrors the configured paths under `forge-it.modules.kafka.path`. Files
resolve relative to the unchangeable `src/test/resources/forge-it` root, for example:

- `defaultUserCreatedEvent.json` for payloads under `/kafka/default/payload`
- `expectedUserCreatedEvent.json` for expected payloads under `/kafka/expected`
- `defaultUserCreatedMetadata.json` for metadata under `/kafka/default/metadata`

### Publishing messages

Use the publish builder to load defaults, mutate payloads/metadata, or send raw fixtures:

```java
forgeit.kafka()
        .publish(UserKafkaContracts.USER_CREATED_INPUT)
        .payload(payload -> payload.setUserId("123"))
        .metadata("defaultUserCreatedMetadata.json", meta -> meta.setSource("tests"))
        .key("user-123")
        .send();
```

### Consuming and asserting messages

The consume builder waits for a message and asserts against expected fixtures:

```java
forgeit.kafka()
        .consume(UserKafkaContracts.USER_CREATED_OUTPUT)
        .await(Duration.ofSeconds(5))
        .assertPayload()
        .assertMetadata("defaultUserCreatedMetadata.json")
        .ignoreFields("payload.createdAt", "metadata.traceId");
```

`assertPayload()` and `assertMetadata()` use the default expected fixture names from the
contract. Use `assertEnvelope(...)` when you want to compare the entire envelope instead
of the extracted payload or metadata.

## Release flow

The repository is set up to automatically cut releases whenever changes are pushed to the
`main` branch. The CI workflow performs the following steps:

1. Reads the project version from the root `pom.xml` and computes both the release
   version (without `-SNAPSHOT`) and the next patch snapshot version.
2. Sets the release version across every module with
   `./mvnw versions:set -DnewVersion=<release> -DgenerateBackupPoms=false`.
3. Runs the full Maven verification and deploys the build to the configured Maven
   repository.
4. Commits the release (`Release X.Y.Z`), tags it as `vX.Y.Z`, and pushes the updates to
   `main`.
5. Creates a sync branch (`sync/release-X.Y.Z`) that bumps the patch version to the next
   `-SNAPSHOT`, commits the change (`Prepare next development version X.Y.(Z+1)-SNAPSHOT`),
   pushes the branch, and opens a pull request to `develop` titled
   `chore: sync X.Y.Z to develop`.

You can reuse the version helper locally via
`.github/scripts/version_helper.py` to inspect the derived versions:

```bash
python .github/scripts/version_helper.py         # prints release/next versions
python .github/scripts/version_helper.py export  # emits shell env vars
```

## External HTTP E2E with the existing MockMvc DSL

`@E2E` creates a small Spring test context for an already running environment. It loads
`application-e2e.yml`, environment/system properties and annotation `properties` overrides,
then installs only the features declared by the test's ForgeIT interface. This release
supports `MockMvcSupport` and `RosSupport` in E2E; other features fail before installation. No application
scan, servlet MockMvc, database cleanup or containers are started by E2E.

```java
public final class ServiceContracts {
    public static final ServiceContract AUTH = ServiceContract.builder()
            .baseUrlFromProperty("consumer.http.auth-base-url")
            .build();
    private ServiceContracts() { }
}

@ForgeFeatures(MockMvcSupport.class)
public interface E2eSupport extends ForgeIT { }

@E2E
class AuthE2E {
    @Autowired private E2eSupport forgeIt;

    @Test void login() {
        forgeIt.mockMvc(ServiceContracts.AUTH)
                .ping(MockMvcEndpoint.loginDefault())
                .assertDefault();
    }
}
```

Import `ServiceContract` from `com.sitionix.forgeit.domain.endpoint` and `E2E` from
`com.sitionix.forgeit.core.test`. The existing annotation processor generates `E2eSupportImpl`.
Reuse the same endpoints, JSON fixtures, defaults, mutators and explicit DSL as IT:

```java
forgeIt.mockMvc(ServiceContracts.AUTH).ping(MockMvcEndpoint.login())
        .withRequest("loginRequest.json")
        .expectResponse("loginResponse.json")
        .expectStatus(HttpStatus.OK)
        .assertAndCreate();
```

Configure the consumer's `application-e2e.yml`:

```yaml
consumer:
  http:
    auth-base-url: ${AUTH_BASE_URL}
forge-it:
  modules:
    mock-mvc:
      connect-timeout: 5s
      request-timeout: 10s
```

Service contracts store address definitions, resolved independently against each test
context when bound. Literal HTTP(S) URLs are also supported in contract definitions.
Missing/unresolved addresses, credentials, query/fragment in a base URL, or invalid ports
fail before I/O. Base paths are retained, path/query values are encoded, and redirects
are not followed. 4xx/5xx responses remain available to normal assertions. Both timeouts
must be between 1 ms and 10 minutes; the request deadline includes the response body.
There are no application retries. Each context owns and closes its HTTP client.

`token(null)` suppresses a default token; an explicit Authorization header takes precedence.
The existing JSON comparison and ignored-field behavior are unchanged. `andExpectPath(ResultMatcher)`
requires real MVC and fails immediately in E2E. Use `mockMvc()` in IT and
`mockMvc(ServiceContracts.AUTH)` in E2E; mixing annotations or transport modes is an error.

### Response duration in IT and E2E

The existing builder optionally asserts performance independently of HTTP timeouts:

```java
forgeIt.mockMvc(ServiceContracts.AUTH)
        .ping(MockMvcEndpoint.loginDefault())
        .expectResponseWithin(Duration.ofSeconds(1))
        .assertDefault();
```

Import `java.time.Duration`. The same method works with `mockMvc()` in IT and with
`assertAndCreate()`. The threshold must be non-null and positive. Equality passes;
exceeding the threshold raises `AssertionError` with the expected maximum duration
and actual duration, at nanosecond precision. Without a threshold, timing is informational.

Both transports measure with `System.nanoTime()` until the full response body is
available (including asynchronous MVC dispatch). Fixture preparation and assertions
are excluded. `expectResponseWithin` never changes `connect-timeout` or
`request-timeout`: a transport timeout is still a transport failure, while a performance
failure follows a completed response.

Every received response, including error statuses and assertion failures, produces one
INFO line from `MockMvcBuilder`, for example:

```text
HTTP POST /auth/login status=200 duration=187ms
```

The log contains the endpoint template, never resolved path parameters, the base URL,
query values, bodies, headers, cookies or tokens. Milliseconds in the log are truncated;
the assertion uses the full measured duration. Transport failures have no response
and therefore emit no response timing line.

Run the library's Docker-free self-tests (JDK 21 and Maven required):

```bash
./scripts/test-e2e-self.sh
```

These tests start local HTTP fixtures on port 0 in their test harness, exercise generated
consumer injection, and check transport parity, configuration isolation, deadlines and
interrupts. Existing optional feature libraries may be present on the classpath; their
installers and services are not activated by E2E. Local smoke success does not establish
that an external platform works.

The consumer `AuthE2E` example is excluded from ordinary Surefire discovery. Run it only
explicitly against an environment you have already started:

```bash
AUTH_BASE_URL=https://your-auth-host/api mvn -pl forge-it-consumer-it -am \
  -Dtest=AuthE2E -Dsurefire.failIfNoSpecifiedTests=false test
```


## ROS 2 messaging

`forge-it-ros` provides a fixture DSL like Kafka while a single context-owned Python
`rclpy` adapter performs ROS transport. Select only the features needed by your test:

```java
@ForgeFeatures(RosSupport.class)
public interface E2eSupport extends ForgeIT { }
```

Use `@E2E` for an infrastructure-only test context or existing IT context installation.
Import the public types from `com.sitionix.forgeit.ros.api`. No ROS Java libraries,
generated Java ROS messages, containers, or ROS installation are bundled.

```java
static final RosTopicContract STATUS = RosTopicContract.builder()
        .topicFromProperty("e2e.ros.status-topic")
        .messageType("my_messages/msg/Status")
        .qos(RosQos.reliableVolatile(10))
        .defaultPublishMessage("starting.json")
        .defaultExpectedMessage("ready.json")
        .build();

forgeIt.ros().publish(STATUS).message("starting.json").publish();
forgeIt.ros().publish(STATUS).publishDefault();

// Asserts the first received message only, even when it mismatches.
forgeIt.ros().consume(STATUS).await(Duration.ofSeconds(5)).assertMessage("ready.json");

// Checks each successive message; one overall deadline includes subscription setup.
forgeIt.ros().consume(STATUS)
        .waitUntilAsserted(Duration.ofSeconds(10))
        .ignoreFields("timestamp", "sequence")
        .assertMessage();
```

Topic contracts are immutable. Configure exactly one of `topic(...)` and
`topicFromProperty(...)`; property values resolve independently per context.
`messageType` is dynamically resolved by ROS as `package/msg/Message`.
QoS is an immutable `RosQos(reliability, durability, history, depth)` record with
`RELIABLE`/`BEST_EFFORT`, `VOLATILE`/`TRANSIENT_LOCAL`, `KEEP_LAST`, positive depth.
Its default is reliable/volatile/keep-last depth 10. Unsupported policies fail explicitly.

Fixtures live under `src/test/resources/forge-it`:

| Operation | Fixture directory |
| --- | --- |
| `message(name).publish()` | `ros/publish/` |
| `publishDefault()` | `ros/default/publish/` |
| `assertMessage(name)` | `ros/expected/` |
| `assertMessage()` | `ros/default/expected/` |

Fixtures and messages must be JSON objects. Assertions use JSONAssert LENIENT,
matching existing ForgeIT semantics: extra fields are allowed, array order is ignored,
and `ignoreFields` removes matching field names recursively. Each streamed message
is compared independently; no first-message cache or growing history exists.
Failure summaries contain structural mismatch counts, never expected/actual values.
Timeouts report the topic template, configured duration, received count and last
assertion summary. `waitUntilAsserted` selects the overall budget; `await` controls
first-message mode and does not override that streaming budget.

```yaml
forge-it:
  modules:
    ros:
      enabled: true
      python-command: python3
      startup-timeout: 10s
      default-consume-timeout: 5s
      domain-id: ${ROS_DOMAIN_ID:}
```

`python-command` is one executable path, not a shell command. Source your ROS install
and any message overlays before starting Maven, or point it at an executable wrapper
that sources them and forwards arguments. A blank domain setting preserves inherited
`ROS_DOMAIN_ID`. Selecting RosSupport while `enabled=false` fails explicitly.
The startup timeout also bounds publisher discovery/acknowledgement. A publisher
requires a matched DDS subscriber before sending; it fails if none is discovered.
Reliable publishers wait for middleware acknowledgement where the installed rclpy
provides it. This is not an application-processing acknowledgement. BEST_EFFORT
retains ROS best-effort delivery semantics.

### Adapter protocol and limits

The internal version-1 protocol is UTF-8 JSON Lines on stdout. Frames contain `type`,
deterministic request IDs (`r1`, `r2`, ...) and subscription IDs (`s1`, `s2`, ...).
`READY` with ID `0` marks startup; command acknowledgements echo the request ID.
Commands: `START_SUBSCRIPTION`, `STOP_SUBSCRIPTION`, `PUBLISH`, `SHUTDOWN`.
Events: `MESSAGE` (subscription ID and JSON message), `READY`, `ERROR` (fixed safe code).
ROS/native logs go to stderr; Java drains and discards them to avoid leaking payloads
or runtime details. Human-readable logs are never parsed as protocol.

One adapter is reused per context and closed with the context. Java has at most 128
pending requests/subscriptions and 64 queued messages per subscription; overflow
fails explicitly rather than evicting the first message. Frames are capped at 1 MiB.
The Python adapter bounds command/publish queues and retains at most 256 publishers
and 256 subscriptions. Reusing topic/type/QoS reuses the publisher. Long-running tests
that exceed those limits must use a fresh context. Startup, malformed protocol,
unknown message types, invalid QoS, process exit and resource exhaustion fail without
fallback. Subscription STOP acknowledgements are tracked asynchronously so cleanup cannot extend
the assertion deadline or replace its result. Missing STOP acknowledgement terminates
the affected adapter after a bounded cleanup timeout. Context shutdown is bounded and
force-kills an unresponsive child as a last resort.

### ROS self-tests

Docker-free protocol, contract, DSL and generated-context tests require Python 3,
but no ROS installation:

```bash
./scripts/test-ros-self.sh
```

Real ROS tests are opt-in, use unique temporary `std_msgs/msg/String` topics, and
cover the first-message assertion, immediate first mismatch, and later matching
message. They start no Gazebo, PX4, Ancestor services or hardware:

```bash
source /opt/ros/lyrical/setup.bash  # or your installed distribution and overlays
ROS_DOMAIN_ID=187 ./scripts/test-ros-real.sh
```

The default Maven suite skips the real test unless `-Dforgeit.ros.real=true` is set.
JSON must be finite and representable by `rosidl_runtime_py` conversion; NaN/Infinity
or unsupported message-field conversions fail explicitly. Services/actions, sequence
history assertions, retries and automatic ROS environment setup are not supported.

### Verify the distributable bundle

After `./mvnw -B -ntp clean install`, run `./scripts/test-bundle-self.sh`.
This separate consumer uses only the `forgeit` test dependency (plus the compiler
annotation processor), checks that every bundled feature installer is whitelisted,
and executes an HTTP E2E with generated support. It resolves the packaged JAR,
not reactor module class directories. CI runs it after the full reactor build.
