# Звіт: ServiceContract та HTTP E2E

- Репозиторій: `sitionix/forge-it`, checkout зареєстрованого source Forge AI.
- Гілка: `feature/SITIONIX-66`.
- BASE_SHA: `e339f2471a9e3fd21163c4b768d766430041c2b0` (перевірений актуальний `origin/develop`).
- HEAD: коміт цієї реалізації та звіту; точний SHA повертає `git rev-parse HEAD` і наведено у фінальній відповіді виконавця.
- PR, push, merge, release та bump версій не виконувалися.

## API та повторне використання

```java
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

`ServiceContracts.AUTH` визначено через `ServiceContract.builder().baseUrlFromProperty("consumer.http.auth-base-url").build()`.
`E2eSupport` використовує наявні `@ForgeFeatures(MockMvcSupport.class)` та `ForgeIT`.
Processor штатно генерує implementation; generated Java вручну не змінювався.

Єдиний `MockMvcBuilder` зберігає fixture/default/mutator logic, token/header/cookie precedence,
flattening query values та portable JSON/status assertions. Повторно використано Endpoint,
MockmvcDefault/Context, MockMvcLoader, JsonLoaderImpl, JsonComparator та consumer auth endpoints/fixtures.
`MvcExecutor` володіє справжнім MVC request/result та виконує ResultMatchers після portable assertions.
`HttpExecutor` обмінюється простими request/response records; synthetic MvcResult немає.
Journal binding повертає новий journal, адреса резолвиться через Environment без зміни контракту чи singleton.

`@E2E` використовує plain Spring context з явною порожньою configuration, ConfigData loading,
наявне визначення contract/features у ContextCustomizerFactory та FeatureInstallationService.
Реєструються JSON infrastructure, selected generated implementation та HTTP feature/client.
Client закривається разом із context. Непідтримувані features та змішані annotations відхиляються
до installation. Залежність core від application стала compile для повторного використання JsonLoaderImpl;
framework/dependency versions не змінювалися.

## Перевірки

Усі команди запускалися з кореня checkout, JDK 21, Maven; `-o` використовує вже завантажені залежності.
Для першого запуску на чистій машині `-o` слід пропустити.

| Команда | Результат |
| --- | --- |
| `mvn -B -pl forge-it-mockmvc -am test` на BASE, потім із characterization test | PASS перед рефакторингом |
| `mvn -o -B -pl forge-it-annotation-processor,forge-it-core -am test` | PASS, 58 unit tests на той момент, включно з усіма тестами змінених library modules та dependency modules |
| `mvn -o -B -pl forge-it-consumer-it -am -Dtest=AuthControllerIT,SelectiveSupportIT -Dsurefire.failIfNoSpecifiedTests=false -Dapi.version=1.40 test` | PASS, 11 існуючих consumer IT |
| `./scripts/test-e2e-self.sh -o` | PASS, 35 self/regression tests: 30 MockMvc module + 5 consumer bootstrap/network tests |
| `git diff --check` | PASS |

Останні Surefire XML звіти містять 75 різних test invocations, 0 failures/errors/skips.
Набори частково перекриваються — кількості рядків таблиці не потрібно додавати.

Покрито literal/property resolution та invalid addresses, два environments для static контракту,
defaults та explicit mutators на MVC/HTTP, token suppression/Authorization, headers/cookies,
path і повторювані query values, UTF-8/reserved encoding/base path, 204/400/500,
redirect policy, assertion mismatch, два interleaved service bindings, invalid header secret redaction,
transport errors, deadline stalled response body, interrupt preservation та відсутність application retries.
Timeout/interrupt tests використовують CountDownLatch, без sleep. HTTP fixtures стартують тільки
з test harness на port 0. Sequential E2E→IT→E2E і повторне використання cached context перевірені.

Під час рев'ю виявлено й виправлено дві регресії із red→green tests: порядок MVC matchers
та витік header values у JDK request validation errors. Повторне read-only рев'ю зауважень не має.

Початковий consumer IT запуск не стартував через Docker API: Testcontainers client використовував 1.32,
Docker 29.8.0 вимагає мінімум 1.40. Лише параметр запуску `-Dapi.version=1.40` усунув проблему;
залежності та Docker daemon не змінювалися. Окремий `-pl forge-it-core -am` не включає processor
у reactor, тому для unit-команди явно додано `forge-it-annotation-processor`.

## Обмеження та що не запускалося

- Зовнішній platform/live `AuthE2E` не запускався: адреса і готове користувацьке середовище не надані.
  Локальний HTTP smoke не є доказом проходження platform E2E.
- `AuthE2E` не відповідає default Surefire patterns; явна команда наведена в README.
- Цей E2E зріз підтримує лише MockMvcSupport HTTP; MVC ResultMatcher є IT-only.
- Connect/request timeout допускають 1 ms–10 min; redirects автоматично не виконуються.
- Наявний глобальний FeatureContextHolder не перероблявся для паралельних contexts; послідовне
  перемикання перевірено, глобальна parallel isolation інших features поза scope.
- Повний набір інших consumer IT (Kafka/Mongo/SQL тощо) не запускався, бо ці функції не змінювалися.
- На classpath consumer залишаються наявні optional module dependencies; E2E не створює їх beans
  і не запускає контейнери, DB cleaner або application-under-test.

## Змінені файли

- `README.md`
- `docs/e2e-implementation-report.md`
- `docs/superpowers/plans/2026-09-18-e2e.md`
- `forge-it-annotations/src/main/java/com/sitionix/forgeit/core/internal/test/E2eTestConfiguration.java`
- `forge-it-annotations/src/main/java/com/sitionix/forgeit/core/internal/test/TestExecutionMode.java`
- `forge-it-annotations/src/main/java/com/sitionix/forgeit/core/test/E2E.java`
- `forge-it-consumer-it/src/test/java/com/sitionix/forgeit/consumer/e2e/AuthE2E.java`
- `forge-it-consumer-it/src/test/java/com/sitionix/forgeit/consumer/e2e/E2eBootstrapTest.java`
- `forge-it-consumer-it/src/test/java/com/sitionix/forgeit/consumer/e2e/E2eSupport.java`
- `forge-it-consumer-it/src/test/java/com/sitionix/forgeit/consumer/e2e/HttpE2eSelfTest.java`
- `forge-it-consumer-it/src/test/java/com/sitionix/forgeit/consumer/e2e/ServiceContracts.java`
- `forge-it-consumer-it/src/test/resources/application-e2e.yml`
- `forge-it-core/pom.xml`
- `forge-it-core/src/main/java/com/sitionix/forgeit/core/internal/test/ForgeE2eContextCustomizer.java`
- `forge-it-core/src/main/java/com/sitionix/forgeit/core/internal/test/IntegrationTestContextCustomizerFactory.java`
- `forge-it-domain/src/main/java/com/sitionix/forgeit/domain/endpoint/ServiceContract.java`
- `forge-it-domain/src/main/java/com/sitionix/forgeit/domain/endpoint/ServiceContractBuilder.java`
- `forge-it-mockmvc/src/main/java/com/sitionix/forgeit/mockmvc/api/MockMvcSupport.java`
- `forge-it-mockmvc/src/main/java/com/sitionix/forgeit/mockmvc/internal/config/MockMvcFeatureInstaller.java`
- `forge-it-mockmvc/src/main/java/com/sitionix/forgeit/mockmvc/internal/config/MockMvcProperties.java`
- `forge-it-mockmvc/src/main/java/com/sitionix/forgeit/mockmvc/internal/domain/MockMvcBuilder.java`
- `forge-it-mockmvc/src/main/java/com/sitionix/forgeit/mockmvc/internal/executor/HttpExecutor.java`
- `forge-it-mockmvc/src/main/java/com/sitionix/forgeit/mockmvc/internal/executor/MockMvcExecutor.java`
- `forge-it-mockmvc/src/main/java/com/sitionix/forgeit/mockmvc/internal/executor/MockMvcRequest.java`
- `forge-it-mockmvc/src/main/java/com/sitionix/forgeit/mockmvc/internal/executor/MockMvcResponse.java`
- `forge-it-mockmvc/src/main/java/com/sitionix/forgeit/mockmvc/internal/executor/MvcExecutor.java`
- `forge-it-mockmvc/src/main/java/com/sitionix/forgeit/mockmvc/internal/executor/ServiceAddress.java`
- `forge-it-mockmvc/src/main/java/com/sitionix/forgeit/mockmvc/internal/journal/MockMvcJournal.java`
- `forge-it-mockmvc/src/main/resources/forge-it-mockmvc-default.yml`
- `forge-it-mockmvc/src/test/java/com/sitionix/forgeit/mockmvc/internal/domain/HttpDeadlineTest.java`
- `forge-it-mockmvc/src/test/java/com/sitionix/forgeit/mockmvc/internal/domain/HttpExecutorTest.java`
- `forge-it-mockmvc/src/test/java/com/sitionix/forgeit/mockmvc/internal/domain/JournalBindingTest.java`
- `forge-it-mockmvc/src/test/java/com/sitionix/forgeit/mockmvc/internal/domain/MockMvcBuilderDefaultsTest.java`
- `forge-it-mockmvc/src/test/java/com/sitionix/forgeit/mockmvc/internal/domain/ServiceContractTest.java`
- `forge-it-mockmvc/src/test/java/com/sitionix/forgeit/mockmvc/internal/domain/TransportParityTest.java`
- `scripts/test-e2e-self.sh`
