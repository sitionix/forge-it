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
    <version>0.0.6-SNAPSHOT</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>com.sitionix.forgeit</groupId>
    <artifactId>forge-it-annotation-processor</artifactId>
    <version>0.0.6-SNAPSHOT</version>
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
