# CLAUDE.md

Guidance for Claude Code (or any AI coding agent) working in this repository: a
native Android (Kotlin + Jetpack Compose) client for a mock REST ticket
marketplace. Full architecture rationale lives in
`documentation/DESIGN.md`; the original plan lives in `documentation/SPEC.md`;
official Jetpack Compose reference material lives in
`documentation/ComposeDocumentation.md`. This file is the short, always-loaded set
of operational rules - read those for anything not covered here.

## Hard rules

- **Never modify anything under `backend/`.** The project explicitly forbids
  changing the backend; treat it as read-only reference (`backend/index.js` for
  routes, `backend/README.md` for the endpoint table, the `.json` files for sample
  data). If something about it seems like it needs to change, note it in
  documentation instead of editing it.
- Domain models (`model/`) are hand-written and independent of the REST API's wire
  DTOs (`network/dto/`). Repositories map between them; nothing outside a repository
  should import a `network.dto` type or the `TicketlyApiService` interface directly.

## Build & run

- Backend (separate terminal, from `backend/`): `npm install && npm start` - runs on
  `localhost:4000` (see `backend/README.md` for the endpoint table and env vars).
- Compile check: `./gradlew compileDebugKotlin`
- Full build: `./gradlew assembleDebug`

## Architecture (see `documentation/DESIGN.md` for full rationale)

Compose UI -> ViewModel -> Repository (interface + Impl) -> Retrofit REST client
(`TicketlyApiService`, backed by OkHttp) -> `backend/`.

### Key Architectural Patterns

- **MVVM**: ViewModels expose UI state as sealed interfaces (`EventsUiState`,
  `DetailUiState`, ...) with a data-class payload for the success case, consumed by
  Compose screens via `collectAsStateWithLifecycle()`.
- **Dependency Injection**: Hilt (built on Dagger). One shared `RepositoryModule`
  binds every repository interface to its implementation - not one module per
  feature.
- **Coroutines/Flow**: async operations are `suspend` functions returning `Result`,
  never throwing. `MutableStateFlow` for discrete/action-triggered state (login,
  checkout). `combine(...).stateIn(viewModelScope, WhileSubscribed(5_000), ...)` for
  state derived from a continuously-changing input (search query, selected date).
- **No use-case/interactor layer** - repositories are called directly from
  ViewModels. Only introduce one if a feature needs to combine multiple repository
  calls into a single rule.
- **Single source of truth for session/cart/wallet**: `AuthRepository.currentUser`
  is the one observable state every screen reads. Any mutation affecting cart/wallet
  writes back through it - never keep a second local copy of that state in a
  ViewModel.
- **One `activeSheet` per screen**: a screen that can show more than one bottom
  sheet uses a single private sealed interface + one sheet host, not a boolean per
  sheet.

## Compose conventions

Before hoisting state in a non-obvious way, or designing a new reusable
composable's API, check `documentation/ComposeDocumentation.md` and fetch the
relevant linked guide rather than relying on general knowledge - these are
load-bearing decisions worth verifying against Google's current guidance.

## Comments

- Explain the system as it currently is, not its history or how it used to work.
- Describe what a class/function does and why it exists, not who calls it.
- Prefer "why" over "what" - skip comments a reader could get from the code itself.
- US English.

## Resolved: date chips are navigational, not a filter

The events list groups by date for display; date chips scroll the already-grouped
list to that day's section (`EventsScreen.kt`'s `dateHeaderIndices`) rather than
filtering it. The per-day `.filter { ... }` line in `EventsViewModel.kt` stays
commented out deliberately - see `DESIGN.md`'s "Events list" section.
