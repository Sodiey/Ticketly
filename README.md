# Ticketly

A native Android (Kotlin + Jetpack Compose) client for a small ticket marketplace:
browse events, log in, add tickets to a cart, check out, and see what you bought.
Built as a personal project, backed by a mock REST API.

## Project layout

- `app/` - the Android client (MVVM: Compose UI -> ViewModel -> Repository -> Retrofit).
- `backend/` - a small Express REST API serving events/tickets/venues from JSON
  files, kept in memory at runtime. See `backend/README.md` for the full endpoint
  table and env vars.
- `scripts/run-backend.sh` - installs the backend's dependencies (first run only)
  and starts it.
- `documentation/` - architecture rationale (`DESIGN.md`), the original plan
  (`SPEC.md`), a testing plan (`TESTING.md`), and Compose reference material.
- `PROMPTS.txt` - a curated log of prompts from the session that built this app.

## Running it

**1. Start the backend** (from the repo root):

```
./scripts/run-backend.sh
```

This runs on `http://localhost:4000` by default. Demo credentials (see
`backend/users.json`): `first` / `user`, `second` / `user`, `third` / `user`.

**2. Run the Android app** on an emulator (it talks to the backend via
`10.0.2.2:4000`, the emulator's alias for the host machine's localhost):

```
./gradlew installDebug
```

or open the project in Android Studio and run the `app` configuration.

## Building

- Compile check: `./gradlew compileDebugKotlin`
- Full build: `./gradlew assembleDebug`
- Tests: `./gradlew testDebugUnitTest connectedDebugAndroidTest`
