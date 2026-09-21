# Ticketly Android App - Design & Implementation Plan

## Goal

Build a small, native Android app (Kotlin + Jetpack Compose) on top of the provided
REST backend: a minimal ticket marketplace. Someone should be able to browse
events, look at what's on offer, log in, put tickets in a cart, pay, and see what
they bought. Scoped to fit in about a day - one flow, done end to end, with
architecture and code quality prioritized over feature breadth.

## Scope

**In scope:**
- Browse events (list + search)
- View a single event's details and its available tickets
- Log in / log out
- Add / remove tickets from a cart
- Check out (pay) and see purchased tickets afterward

**Out of scope, deliberately:**
- Saved credit card reuse (`saveCreditCard`) - checkout alone already proves the
  payment path; a second persistence mechanism for the same concern doesn't add
  proof of ability, just time.
- Local persistence (Room/DataStore) - session lives in memory only. A fresh app
  launch means a fresh login, which is an accurate reflection of the backend's own
  in-memory session model, not a missing feature.
- Pagination / infinite scroll - the dataset is small enough not to need it.
- Push notifications, deep links, offline support - none of these demonstrate
  anything relevant to what this project is meant to show off.

## Screens

- **Events (home).** Search bar and a profile icon in the header (opens login, or
  account details once signed in). Below that, a lightweight filter row ("All
  venues" + a filter icon - placeholders for now, not wired to real filtering logic)
  and a horizontal row of date chips. The event list itself is grouped by date, with
  a header per day, so everything is visible in one scroll rather than one day at a
  time.
- **Event Detail.** Pushed on top of Events (its own back button).
  Image, title, description, venue, and one row per ticket type with a Buy button.
  Tapping Buy while signed out opens the login sheet instead of buying; once signed
  in, Buy adds the ticket to the cart and moves straight to the Cart screen.
- **Cart.** Line items with a Remove action, and a Checkout button that opens a
  small sheet asking for a card number before calling the checkout endpoint. Shows a
  login prompt instead of the cart contents when signed out.
- **Login / Account.** Not a separate screen - a bottom sheet, reused everywhere a
  login affordance is needed (Events, Cart, Detail) instead of duplicated per
  screen. Shows a login form when signed out; name, purchased tickets, and a logout
  button when signed in.

## Architecture

Compose UI -> ViewModel -> Repository (interface + impl) -> Retrofit REST client.

- **No use-case/interactor layer.** Every operation here is a direct pass-through to
  one REST call - fetch events, log in, mutate the cart. Wrapping each in its own
  use-case class would add a layer of indirection without adding behavior. Worth
  introducing one later only if a feature needs to combine several repository calls
  into a single business rule.
- **One repository interface + impl per feature area** (events, auth, cart), each
  mapping the REST API's wire DTOs into hand-written domain models. Keeps network
  response shapes out of the ViewModel/UI layer entirely.
- **Session state as a single observable source of truth.** `AuthRepository`
  exposes `currentUser` as something every screen can read directly; nothing
  re-checks the backend to answer "is someone logged in" - only actual protected
  mutations (cart, checkout) go over the network. Any mutation that changes cart or
  wallet contents writes its result back into that same source, so every screen
  observing it reflects the change immediately, regardless of which screen
  triggered it.
- **One "active sheet" state per screen**, not a boolean per sheet, for any screen
  that can show more than one bottom sheet (Events: filter, login, account) - keeps
  the pattern from sprawling as more sheets get added later.

## Navigation

Bottom nav with two tabs: Home (Events) and Cart. Detail is pushed on top of Events
and hides the bottom bar. Home always resets to a clean Events screen rather than
restoring whatever was on top the last time it was left - tapping Home should mean
"go to Events," full stop, even if Detail was open a moment ago. After a successful
checkout, navigate straight to Cart.

## Data notes worth deciding upfront

- The date-chip row's range should reflect the data actually being shown - anchored
  on the earliest fetched event - rather than always starting from today, since the
  mock dataset's event dates won't necessarily line up with "today," and a chip row
  starting from today would mostly point at empty days.
- Whether the date chips also filter the list down to a single day, or stay a
  purely visual/navigational aid over the always-grouped list, is left open for now
  - the UI supports the chip row either way, and the filtering behavior can be
  decided once it's been tried against real data.
- Cart and wallet contents live on the same `User` object the session state already
  tracks, rather than as separate local state per screen - one place owns "what's in
  the cart," everything else just reads it.

## Definition of done

A user can go from opening the app to holding a purchased ticket, with no dead ends,
using only the flows above. Every screen has a defined loading, empty, and error
state. `backend/PROMPTS.txt` records the prompt used to generate the backend, for
reference.
