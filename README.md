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

#### Fixture locations for WireMock JSON
Request and response payloads resolve relative to the configured `mapping.request` and
`mapping.response` paths (defaults: `/wiremock/request` and `/wiremock/response` under
`src/test/resources/forge-it`). Default payloads consumed by `applyDefault(...)` and
`createDefault(...)` load from `mapping.default-request` and `mapping.default-response`.
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

Request and response JSON files live under `src/test/resources/forge-it` by default, so
`withRequest("loginRequest.json")` resolves to `forge-it/mockmvc/request/loginRequest.json`.
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

Add `andExpectPath(...)` for extra matchers, or `token(...)` to attach an `Authorization`
header. Response assertions can ignore fields via `expectResponse(..., fieldsToIgnore...)`.

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
                    .withRequest("loginRequest.json")
                    .expectResponse("loginResponse.json")
                    .expectStatus(200)
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

### Configuration
The PostgreSQL feature starts a `postgres:16-alpine` Testcontainers instance by default and
initialises schema/constraints/data from SQL under `/db/postgresql` (see the consumer
fixtures in `forge-it-consumer-it/src/test/resources/forge-it/db/postgresql/**`). Override
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

### Schema, constraints, and seed scripts
SQL scripts execute deterministically: everything under `schema/`, then `constraints/`,
then `data/`, and finally any other folder (treated as `custom`). Inside a phase,
filenames are ordered by their leading number, so `001_create_users.sql` runs before
`110_add_fk.sql`; files without a numeric prefix run last within the phase. Drop your own
DDL, constraint, and seed files under `src/test/resources/forge-it/db/postgresql/**` (or
the path set in `forge-it.modules.postgresql.paths.ddl.path`) to extend the database for
your tests. The sample consumer illustrates the layout:

- `schema/*.sql` for table creation
- `constraints/*.sql` for foreign keys, unique indexes, or checks
- `data/*.sql` for reference or seed rows
- any other folder for custom scripts that should run after data loads

### Entity fixtures and JSON mapping
Entities can be hydrated from JSON rather than hand-built objects. Default bodies declared
via `.withDefaultBody(...)` load from `forge-it.modules.postgresql.paths.entity.defaults`
(defaults to `/db/postgresql/entities/default`), while `.withJson(...)` pulls from
`forge-it.modules.postgresql.paths.entity.custom` (default `/db/postgresql/entities/custom`).
Resources are resolved under `src/test/resources/forge-it`, so
`withDefaultBody("default_user_entity.json")` maps to
`forge-it/db/postgresql/entities/default/default_user_entity.json`, and
`withJson("priority_user_entity.json")` maps to
`forge-it/db/postgresql/entities/custom/priority_user_entity.json`. Override the paths if
you prefer a different folder structure. Keep JSON aligned with the entity structure that
Jackson deserialises; combine shared defaults with scenario-specific custom files to avoid
duplicating base shapes.

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
```

### Cleanup, verification, and transactions
- `@IntegrationTest` registers a cleanup listener that executes `DELETE_ALL` contracts
  after each test; override phases with `cleanupPhase` or `@DbCleanup` when you need
  different timing.
- `CleanupPolicy.NONE` leaves reference data intact between tests; keep lookups (e.g.,
  statuses) on this policy and dependents on `DELETE_ALL`.
- Use `forgeit.postgresql().get(Entity.class)` to verify rows (`getAll()`, `getById(id)`)
  and `DbGraphResult` to assert on freshly persisted entities. When the same contract
  is invoked multiple times, `entity(contract)` returns the most recently stored one;
  use labels on the invocation to retrieve a specific instance.
- Transaction boundaries for graph execution are controlled by `tx-policy`
  (`REQUIRES_NEW` by default). Choose `MANDATORY` if you want to reuse an outer
  `@Transactional` block.

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
