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

## WireMock support

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

### Configuration

The module ships with sensible defaults for loading request and response payloads from the
classpath. Override the locations if your project stores fixtures elsewhere:

```yaml
forge-it:
  modules:
    mock-mvc:
      path:
        request: /mockmvc/request         # standard request fixture folder
        response: /mockmvc/response       # standard response fixture folder
        default-request: /mockmvc/default/request
        default-response: /mockmvc/default/response
```

### Executing requests

Use `forgeit.mockMvc().ping(...)` with an `Endpoint` definition to drive the request.
Request/response bodies are loaded from the configured folders and can be mutated before
execution:

```java
forgeit.mockMvc()
        .ping(MockMvcEndpoint.login())
        .request("loginRequest.json", req -> {
            req.setUsername("username");
            req.setPassword("password");
        })
        .response("loginResponse.json", res -> res.setToken("mutated-token"))
        .status(HttpStatus.OK)
        .execute();
```

Add `andExpectPath(...)` for extra matchers, or `token(...)` to attach an `Authorization`
header. Response assertions can ignore fields via `response(..., fieldsToIgnore...)`.

### Defaults and reusable fixtures

Endpoints can declare defaults (request, response, status) that the builder reuses via
`executeDefault()`. You can further mutate the defaults at call time:

```java
// Endpoint with baked-in defaults
public static Endpoint<LoginRequest, LoginResponse> loginDefault() {
    return Endpoint.createContract(
            "/auth/login",
            HttpMethod.POST,
            LoginRequest.class,
            LoginResponse.class,
            (MockmvcDefault) ctx -> ctx
                    .request("loginRequest.json")
                    .response("loginResponse.json")
                    .status(200)
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

`PostgresqlSupport` drives relational test data through JSON fixtures, contract graphs,
and optional Testcontainers orchestration. It mirrors the patterns used in WireMock and
MockMvc so that API stubs, database seeding, and cleanup share the same lifecycle.

### Configuration

Defaults are provided by `forge-it-postgresql-default.yml`, but every key can be
overridden in your test configuration:

```yaml
forge-it:
  modules:
    postgresql:
      enabled: true              # disable to skip the module entirely
      mode: internal             # or "external" to reuse an existing instance
      container:
        image: postgres:16-alpine
      connection:
        database: forge-it
        username: forge-it
        password: forge-it-pwd
        host: localhost          # required when mode is external
        port: 5432               # required when mode is external
        jdbc-url: jdbc:postgresql://localhost:5432/forge-it
      paths:
        entity:
          defaults: /db/postgresql/entities/default  # default JSON payloads
          custom: /db/postgresql/entities/custom     # overrides for specific scenarios
        ddl:
          path: /db/postgresql                       # root folder for schema SQL files
      tx-policy: requires_new   # REQUIRED, REQUIRES_NEW, or MANDATORY
```

- **Internal mode** starts a `postgres:16-alpine` Testcontainer and publishes
  `forge-it.postgresql.connection.*` properties (JDBC URL, host, port, credentials)
  into the Spring Environment. The module shuts the container down on context close.
- **External mode** reuses an existing database; you must provide `host` and `port`
  (or a `jdbc-url`) under `connection`.
- **Schema initialization** runs on `ApplicationReadyEvent` when `paths.ddl.path` is
  set. All SQL files under the configured folder are executed against the active
  `DataSource` via `SqlScriptExecutor`.
- **Transaction policy** controls how the graph executor participates in Spring
  transactions. `MANDATORY` requires an existing transaction (otherwise a
  `ForgeItConfigurationException` is thrown), while `REQUIRED`/`REQUIRES_NEW` wrap the
  graph execution in a `TransactionTemplate` with the matching propagation level.

### Declaring DB contracts

Contracts describe how to build entities from fixtures and how to wire dependencies.
Use `DbContractsDsl` to keep these declarations in one place:

```java
import static com.sitionix.forgeit.domain.contract.DbContractsDsl.entity;

public final class DbContracts {
    public static final DbContract<UserEntity> USER = entity(UserEntity.class)
            .withDefaultBody("user.json")
            .cleanupPolicy(CleanupPolicy.DELETE_ALL) // include in cleanup cycle
            .build();

    public static final DbContract<AddressEntity> ADDRESS = entity(AddressEntity.class)
            .dependsOn(USER, AddressEntity::setUser) // auto-attach parent
            .withDefaultBody("address.json")
            .build();

    private DbContracts() {}
}
```

- `withDefaultBody(...)` points at a JSON file under the `paths.entity.defaults`
  folder; `withJson(...)` on the invocation overrides it with a file from the
  `paths.entity.custom` folder.
- `dependsOn(...)` wires parent/child relations; dependencies are resolved and attached
  when the graph is built.
- `cleanupPolicy(CleanupPolicy.NONE)` opts an entity out of automatic table truncation
  while `DELETE_ALL` (the default) includes it.

### Building graphs and seeding data

Use the fluent graph builder to chain contract invocations and persist them inside the
configured transaction boundary:

```java
@ForgeFeatures(PostgresqlSupport.class)
class UserFlowTests implements ForgeIT {

    @Test
    void shouldPersistUserGraph() {
        final DbGraphResult graph = this.postgresql().create()
                .to(DbContracts.USER.withJson("custom-user.json"))
                .to(DbContracts.ADDRESS) // uses the default body and attaches USER
                .build();

        final UserEntity user = graph.entity(DbContracts.USER); // managed entity
        final AddressEntity address = graph.entity(DbContracts.ADDRESS);
        // exercise the SUT using the persisted data...
    }
}
```

Invocation options mirror the contract body specification:

- `contract.withJson("user-overrides.json")` loads a custom fixture.
- `contract.withEntity(existingUser)` uses a pre-built entity instance instead of a
  JSON file.
- `contract.getById(id)` pulls an existing row via `EntityManager#find` when you want
  to reference previously seeded data.

The builder caches entities per contract during a single `build()` call, so multiple
children sharing a parent receive the same managed instance. After the graph executes,
entities are merged into the active `EntityManager` and flushed.

### Reading data back in tests

`postgresql().get(...)` exposes a thin retrieval API on top of JPA for quick assertions:

```java
final DbRetriever<UserEntity> users = forgeit.postgresql().get(UserEntity.class);
assertThat(users.getById(1L)).isNotNull();
assertThat(users.getAll()).hasSize(3);
```

### Cleanup strategies

The default `@IntegrationTest` meta-annotation runs database cleanup **after each**
test via `ForgeItDbCleanupTestExecutionListener`, which gathers all known contracts
from `DbContractsRegistry` and deletes rows for contracts whose `cleanupPolicy` is
`DELETE_ALL`.

- Override the lifecycle per test/class with `@DbCleanup(phase = CleanupPhase.BEFORE_ALL | BEFORE_EACH | AFTER_EACH | AFTER_ALL | NONE)`.
- Mark specific contracts with `CleanupPolicy.NONE` to exclude them from truncation.
- Trigger a manual cleanup by calling `forgeit.postgresql().clearAllData(contracts)` if
  you need to wipe a subset of contracts mid-test.

Because cleanup runs in a new transaction and issues JPQL `DELETE FROM <Entity>` per
unique entity type, keep your contracts registry complete so that every table seeded in
tests is accounted for.

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
