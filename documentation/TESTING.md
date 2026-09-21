# TESTING.md

Testing plan for the Ticketly Android client. Scoped to what's worth building for a
small side project: unit tests for the two ViewModels that carry real logic, the
pure-function utilities, and Compose UI tests for the three screens' visual states. No
mocking library, no Hilt/DI in the instrumented tests - see "Explicitly out of scope"
for what a fuller suite would add later.

## Test types & tools

Two layers, matching the two test source sets already scaffolded (`app/src/test`,
`app/src/androidTest`):

| Layer | Where | Tools |
|---|---|---|
| Unit (ViewModels, pure functions) | `app/src/test` | JUnit4, `kotlinx-coroutines-test`, Turbine, hand-written fakes |
| Instrumented (Compose screen states) | `app/src/androidTest` | Compose UI test only - no Hilt, no navigation |

No mocking library (MockK/Mockito) is added. The codebase already separates every
repository into an interface + Impl specifically so callers depend on the interface -
that seam is exactly where a hand-written fake belongs, and fakes read clearer than
mock-verification chains for the handful of methods each interface has.

The instrumented layer deliberately stays out of Hilt/DI/navigation entirely: every
screen composable (`EventsScreen`, `DetailScreen`, `CartScreen`) already takes its
`UiState` and other rendering inputs as plain parameters, with a separate `*Route`
wrapper doing the `hiltViewModel()`/`collectAsStateWithLifecycle()` work. That split
means a UI test can call `EventsScreen(uiState = EventsUiState.Loading)` directly
inside `composeTestRule.setContent { }` and assert on what's rendered, with no
ViewModel, fake repository, or DI graph involved at all.

## Fakes needed (one file each, under `app/src/test/.../repository/fakes/`)

- **`FakeEventsRepository`** - backed by a mutable `List<Event>` and a settable
  "next result should fail" flag per method (`getEvents`, `getEvent`, `searchEvents`,
  `getCachedEvents`), so a test can assert both the success and failure path without
  touching the network layer.
- **`FakeCartRepository`** - only `addTicket` is actually exercised (by
  `DetailViewModel`'s tests below), but the fake implements the full interface with a
  settable result, same shape as `FakeEventsRepository`.

## Unit tests: EventsViewModel

- `getEvents()` is called (not `searchEvents`) when the query is blank.
- `searchEvents(query.trim())` is called once the query is non-blank, with the query
  trimmed.
- Debounce: rapid successive `onSearchQueryChanged` calls within the debounce window
  only trigger one repository call, for the final value (Turbine + `runTest`'s virtual
  time).
- `flatMapLatest` cancellation: changing the query while a search is in flight never
  emits the stale result - only the latest query's result reaches `uiState`.
- Failure path: repository failure surfaces as `EventsUiState.Failure`.
- `EventsUiData.eventsByDate` groups and sorts by day correctly given out-of-order
  input.
- `EventsUiData.visibleDates` anchors on the earliest event's date, not
  `LocalDate.now()`, and falls back to today when the list is empty.
- **Do not** write a test asserting the per-day filter narrows `uiState`'s events -
  that line is commented out (`EventsViewModel.kt:52`) and disabled on purpose; see
  "Known open item" below before touching this at all.

## Unit tests: DetailViewModel

- Success path populates `uiState` with the fetched event and its similar events.
- `getEvent` failure surfaces as `DetailUiState.Failure`.
- `getCachedEvents` failure does **not** fail the screen - `similarEvents` is just
  empty (per `DetailViewModel.kt:44`).
- `similarTo` genre matching (`DetailViewModel.kt:88`): same first-word title
  matches, different first word doesn't, the event itself is excluded, and the
  result is capped at 5.
- `addTicket` success moves `addToCartState` to `Idle` and emits once on
  `navigateToCart` (Turbine `test { }` on the `SharedFlow`).
- `addTicket` failure surfaces the repository's error message via
  `AddToCartUiState.Error`.

## Unit tests: pure functions

Cheap, high-value, no fakes needed:

- **`DateUtils.kt`**: `upcomingDates` entry count and boundary (inclusive of both
  ends) for a few `months` values; both `toLocalDate` overloads against known
  instants/strings, including a case that crosses a day boundary depending on zone,
  to pin down `ZoneId.systemDefault()` behavior in CI.
- **`MoneyUtils.kt`**: `formatPrice` for a whole-currency amount, a sub-100-cents
  amount (needs zero-padding), and a currency code, confirming the locale-fixed
  format (`Locale.US`) doesn't drift with the test runner's default locale.

## Instrumented UI tests: Compose screen states

One test class per screen, each rendering the plain screen composable directly with a
hardcoded state and asserting on what's displayed - no ViewModel, no Hilt, no
navigation, no repository (fake or real) involved:

- **`EventsScreenTest.kt`**: `Loading` shows the loading indicator
  (`testTag("circular_loading_indicator")`); `Success` with events shows the event's
  title; `Success` with an empty list shows "No events found"; `Failure` shows
  "Something went wrong".
- **`DetailScreenTest.kt`**: `Loading` shows the loading indicator; `Success` shows
  the event's title and a "Buy" button; `Success` with a zero-`quantityAvailable`
  ticket shows "Sold out" instead; `Failure` shows "Something went wrong".
- **`CartScreenTest.kt`**: `user = null` shows "Log in to view your cart"; a signed-in
  user with an empty `cart` shows "Your cart is empty"; a signed-in user with cart
  lines shows the line's event title, ticket label, and a "Checkout" button. (No
  `Loading` state here - `CartScreen` derives what to show from `user`/`cart`
  directly, it doesn't have one.)

Each test uses `createAndroidComposeRule<ComponentActivity>()` (a bare activity, not
`MainActivity`) and calls `composeTestRule.setContent { ScreenName(uiState = ...) }`
directly, mirroring the fixtures already used by each screen's own `@Preview`
functions. Login, checkout, search/filter interaction, and navigation between screens
are not covered here - see "Explicitly out of scope."

## Explicitly out of scope (for now)

- **`CartViewModel` / `AuthViewModel` unit tests**: both are thin pass-throughs over
  `AuthRepository.currentUser` and a couple of repository calls - lower value per
  test written than `EventsViewModel`/`DetailViewModel`, which carry the actual
  logic (debounce, cancellation, derived state, genre matching). Cut first to keep
  the suite focused on the highest-value coverage.
- **End-to-end / navigation instrumented tests** (e.g. browse → detail → add to cart →
  cart shows the ticket, login, checkout): would need Hilt/DI wiring
  (`hilt-android-testing`, a `@TestInstallIn` module, fakes with real
  cart/auth write-through behavior) and real navigation through `MainActivity`. Cut in
  favor of the plain per-screen-state tests above, which cover the same rendered UI
  states at a fraction of the setup and runtime cost; the ViewModel unit tests already
  cover the business logic that would otherwise justify the extra weight.
- **`*RepositoryImpl` (REST DTO mapping)**: not unit tested. Testing this properly
  would mean adding an HTTP mock-server dependency (e.g. OkHttp's `MockWebServer`),
  which isn't currently in the project. Coverage instead comes from running the app
  against the real local backend (`npm start` in `backend/`) during manual
  verification.
- **`SessionManager`, DI wiring (`RepositoryModule`), `NavGraph`**: thin plumbing,
  not worth a dedicated test.

## Known open item: the disabled per-day filter

`EventsViewModel.kt:52` has a commented-out `.filter { it.startsAt.toLocalDate() ==
date }` line. Per `CLAUDE.md` and `SPEC.md`'s "Data notes" section, whether date
chips filter the list is an open product decision, not a bug. The test plan above
only covers the *current*, shipped behavior (chips are navigational only, `uiState`
shows the full result set). If/when that filter is turned on, add a test asserting
`uiState` narrows to only events on `_selectedDate` once enabled - re-read `SPEC.md`
first, since date-chip range and filtering are meant to interact deliberately.

## Suggested order of implementation

1. `DateUtils`/`MoneyUtils` unit tests (no fakes needed, fastest to land).
2. `FakeEventsRepository`, `FakeCartRepository`.
3. `EventsViewModel` tests (most logic: debounce, cancellation, derived state).
4. `DetailViewModel` tests.
5. `EventsScreenTest`, `DetailScreenTest`, `CartScreenTest` (independent of the above -
   can be done in any order, or in parallel with 3-4).
