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
    <version>0.0.5-SNAPSHOT</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>com.sitionix.forgeit</groupId>
    <artifactId>forge-it-annotation-processor</artifactId>
    <version>0.0.5-SNAPSHOT</version>
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
        request: /mappings/request
        response: /mappings/response
        default-request: /default/request
        default-response: /default/response
```

When `mode` is `internal`, the module launches a `wiremock/wiremock:3.6.0` container and
publishes the base URL as `forgeit.wiremock.base-url`, `forgeit.wiremock.port`, and
`forgeit.wiremock.host` environment properties.

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
