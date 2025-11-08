# ForgeIT Architecture Overview

ForgeIT is organised as a multi-module Maven build that follows an onion-style layering.
Consumers compile against the outer API surfaces while feature modules and runtime
infrastructure live behind internal boundaries.

```
+----------------------+---------------------------------------+
| Module               | Responsibility                        |
+======================+=======================================+
| forge-it-annotations | Declarative annotations used by       |
|                      | consumers to request features.        |
+----------------------+---------------------------------------+
| forge-it-core        | Public ForgeIT entry point (`core/api`|
|                      | package) and integration tests. All   |
|                      | runtime wiring sits in internal       |
|                      | packages.                             |
+----------------------+---------------------------------------+
| forge-it-wiremock    | WireMock feature module. The `api`    |
|                      | package exposes contracts while the   |
|                      | `internal` package houses the actual  |
|                      | implementation.                       |
+----------------------+---------------------------------------+
| forge-it-annotation- | Compile-time generator that reads     |
| processor            | `@ForgeFeatures` declarations and     |
|                      | produces `ForgeITFeatures`, which the |
|                      | public `ForgeIT` interface extends.   |
+----------------------+---------------------------------------+
| forge-it-bundle      | Convenience bundle that aggregates    |
|                      | all required modules for consumers.   |
+----------------------+---------------------------------------+
```

## Communication flow

1. **Consumer ➜ Core API** – Client code implements interfaces that extend
   `com.sitionix.forgeit.core.api.ForgeIT`. This is the only package meant for
   direct use.
2. **Core API ➜ Generated features** – The annotation processor produces the
   `com.sitionix.forgeit.core.generated.ForgeITFeatures` interface during
   compilation. `ForgeIT` extends this interface so every consumer implementation
   inherits all requested features. The processor aggregates feature interfaces
   declared via `@ForgeFeatures` annotations. All legal feature contracts must be
   registered via `META-INF/forge-it/features` resources that ship with feature
   modules; the processor rejects unknown interfaces to prevent consumers from
   accidentally pointing at unsupported implementations. The public `ForgeIT`
   contract is annotated with the built-in WireMock support so that the
   published artefact already exposes the bundled helpers, while consumers can
   add further annotations on their own entry points to opt into additional
   modules.
3. **Generated features ➜ Feature modules** – Each feature module (e.g. WireMock)
   contributes support interfaces under its own `api` package. Implementations live
   in matching `internal` packages and are invoked through default methods or
   bridges so that consumers never touch infrastructure classes directly.

By keeping the outer packages (`core.api`, `*.api`) small and documenting that the
`internal` packages are off-limits, we preserve a clear layering that mirrors the
onion architecture: consumers depend only on the API ring, while the inner rings
(hosting infrastructure and implementation) can evolve independently.
