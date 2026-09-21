# Ticketly Android App - Design Document

This document is the running contract for this project's architecture: the decisions
already made and why, the conventions to follow, and the process for proposing new
features before writing code for them. It exists because building this feature-by-feature
in conversation (reactive, no upfront plan) caused at least one real bug - see
[Open Issues](#open-issues) - where two features quietly assumed different things about
the same piece of state. The goal of this doc is to make that kind of conflict visible
*before* implementation, not after.

## Architecture Overview

```
Compose UI (Screen)
      |
   ViewModel        <- exposes UI state (StateFlow), owns presentation logic
      |
  Repository (interface + Impl)   <- one per feature area, talks to Retrofit
      |
  Retrofit client -> REST backend (backend/)
```

- **No use-case layer.** Repositories are called directly from ViewModels. A use-case
  layer was deliberately skipped for operations that are simple pass-throughs
  (fetch events, log in, add to cart) - introducing one would add indirection without
  adding behavior. If a piece of business logic ever needs to be shared across multiple
  ViewModels *and* is non-trivial, that's the trigger to reconsider this.
- **Domain models are hand-written**, independent of the REST API's wire DTOs
  (`model/Event.kt`, `model/User.kt`, `model/CartLine.kt`, `model/OwnedTicket.kt`, etc.).
  Repositories map `network/dto/`'s `@Serializable` response shapes into these. This
  keeps JSON response shapes from leaking into ViewModels/UI, and means a backend
  field rename only touches the repository doing the mapping.
- **Result<T> for suspend operations that can fail** (network calls). ViewModels turn
  `Result` into UI state (loading/error) at the call site; repositories never throw.


### Auth & session state
- `SessionManager` holds the access token in memory (`StateFlow<String?>`), nothing
  persisted to disk - a fresh app launch is always logged out.
- `AuthRepository` (`@Singleton`) is the single source of truth for "who is the
  current user," exposed as `currentUser: StateFlow<User?>`. Every screen observes
  this directly; **nothing polls the backend to check auth state** - only actual
  protected mutations (add to cart, checkout, etc.) hit the network.
- `AuthViewModel` is a thin Compose-facing wrapper around `AuthRepository` (login/logout
  + loading/error state for the login sheet). It's intentionally instantiated per
  screen via `hiltViewModel()` (Events, Cart, and the Detail app bar each get their own
  instance) rather than forced into a single shared instance - since the state they all
  read from (`AuthRepository.currentUser`) is already a singleton, multiple thin
  ViewModel instances stay consistent for free, and forcing one shared instance would
  need explicit `ViewModelStoreOwner` scoping for no real benefit.

### Cart & wallet state
- `CartRepository` is stateless - just two REST calls (`addTicket`/`removeTicket`)
  that return the fresh cart from the response body. It does **not** keep its
  own copy of the cart.
- The cart (and ticket wallet) live on the domain `User` model
  (`User.cart`, `User.ticketWallet`), and `AuthRepository` is the only thing allowed to
  mutate them (`updateCart()`, `addToWallet()`). `CartRepository` calls those after a
  successful mutation.
  - **Why:** the first implementation gave `CartViewModel` its own local cart override.
    That broke as soon as a ticket was bought from a *different* screen (Detail) than the
    one showing the cart (Cart screen) - the two ViewModels' local state diverged, and the
    bug only became visible after a relogin refetched the real data. Routing every cart
    mutation through the single `AuthRepository.currentUser` StateFlow means any screen
    observing it sees the update immediately, regardless of which screen triggered it.
- Checkout follows the same pattern: `CartRepository.checkout()` clears the local cart
  and appends the response's `purchasedTickets` to the wallet, both via `AuthRepository`.

### Bottom sheets
- A screen showing more than one bottom sheet uses a single `activeSheet` (private
  sealed interface) + one `ModalBottomSheet` host, instead of one boolean/state pair per
  sheet. Sheet *content* is a separate composable (`LoginSheetContent`,
  `AccountSheetContent`, `FilterSheetContent`, `CheckoutSheetContent`) so it can be
  reused both by a screen's unified host and by a standalone full-sheet wrapper
  (`LoginBottomSheet`) where a screen only ever shows one sheet (e.g. Cart's logged-out
  state).
- `AuthMenu` (icon + its own Login/Account sheet) is a self-contained version of this
  pattern for hosts that don't otherwise manage a sheet (MainActivity's Detail app bar).
  It deliberately does **not** share sheet state with `EventsScreen`'s own unified host -
  Detail has no Filter sheet to unify with, and forcing them together would reintroduce
  the exact per-sheet-state duplication this pattern exists to avoid.

### Navigation
- The bottom bar's Home button always does a hard `popUpTo(start){ inclusive = true }`
  reset to the Events screen - it does not use the standard "switch tab, save/restore
  state" pattern, because that pattern restores whatever was pushed on top the last time
  you left (e.g. the Detail screen), which is not what "go home" should ever do.
- `navigateToCart()` (used after a successful buy) is a plain forward push - Detail
  stays on the back stack underneath, so Cart's own back button returns to it. This
  is safe specifically because Home's hard reset above doesn't depend on the stack
  being clear to begin with; it resets unconditionally either way.

### Events list
- `EventsUiData` (in `EventsViewModel.kt`) computes `eventsByDate` (grouped-for-display)
  and `visibleDates` (date-chip range, anchored on the earliest fetched event) once, as
  computed properties evaluated when the ViewModel constructs new state - not inside the
  Composable, which would recompute on every recomposition and doesn't scale with a
  larger list.

## Conventions

- **StateFlow, not LiveData**, everywhere in ViewModels.
- **`MutableStateFlow` for discrete/action-triggered state** (e.g. `loginState`,
  `checkoutState`) - there's no natural upstream `Flow` to derive a login attempt from.
- **`combine` + `.stateIn(viewModelScope, WhileSubscribed(5_000), ...)` for state derived
  from a continuously-changing input** (e.g. `EventsViewModel.uiState` combining search
  results with the selected date).
- Package layout: `presentation/<feature>/` (ViewModel + Screen + `navigation/`),
  `repository/` (interface + Impl per feature), `model/` (domain models), `ui/components/`
  (shared/reusable composables), `di/` (Hilt modules).

## Open Issues

### Events: date filter vs. date grouping
`EventsViewModel` currently groups events by date for display (multiple date headers,
scrollable list showing everything) **and** has a per-day filter (narrows the list down
to a single selected date) that can be toggled on. These two features are in tension:
turning the filter on collapses the grouped list back down to at most one date's worth
of events, which mostly defeats the point of grouping, and - because the mock backend's
event dates are largely in the past relative to "today" - often shows nothing at all.

Not resolved yet. The real fix needs a clear "pipeline" written down before more code
changes:

1. **Fetch** - full, unfiltered `List<Event>` from the repository.
2. **Anchor** - the earliest date in that full list, used for the date-chip range. Must
   always read the *full* list, never a filtered subset.
3. **Filter** *(if kept)* - narrow to the selected day, for what's displayed.
4. **Group** - organize whatever's being displayed by date, for the section headers.

Still an open product question, not just an implementation one: should tapping a date
chip filter the list (original intent) or scroll the already-grouped list to that
date's section (no filtering, always shows everything)? This should be settled before
resuming that work.

## Process For New Features

Before implementing anything beyond a small, contained change (a button, a copy tweak,
a one-file fix), write a short plan here (or as a throwaway note in conversation) covering:

1. **What state does this feature need**, and where does it live (ViewModel? Repository?
   Does it need to be a single shared source of truth the way cart/wallet do?)
2. **What does it read from / write to that already exists** - does it touch state
   another feature also owns or derives from? (This is exactly the check that was
   skipped for the events pipeline above.)
3. **What's explicitly out of scope for now.**

This doesn't need to be heavyweight - a few bullet points is enough for most features.
It exists to force the "does this conflict with something that already exists" question
to happen before code, not after a bug surfaces.
