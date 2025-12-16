# ForgeIT testing notes

* Integration tests rely on deterministic database cleanup after each test method. Cleanup is driven by the `@IntegrationTest` meta-annotation, which registers a cleanup listener that clears all registered contracts once a test finishes.
* Tests no longer depend on Spring's `@Transactional` rollbacks. Database mutations happen inside the graph executor's own transaction and the cleanup listener removes any persisted data between tests.
* If a test needs a different cleanup phase, override `cleanupPhase` on `@IntegrationTest` or add `@DbCleanup` to the test method.
