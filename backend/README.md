# marketplace-backend

A small REST API that simulates a minimal marketplace for buying tickets: browsing
events and venues, logging in, adding tickets to a cart, checking out, and viewing a
wallet of purchased tickets. There's no database — everything is served from the JSON
files in this repo and kept in memory at runtime.

## Data source

All data returned by the API is read at startup from one of the json files:

- `data.json`: events and their available tickets.
- `users.json`: registered users; there is no registration, only login with an existing user.
- `venues.json`: venues.

## How to run 

```
npm install
npm start
```

The server listens on `http://localhost:4000` by default (see `PORT` below). Static images
for events/venues are served from `/images/<file>`.

## Environment variables

Copy `.env.example` to `.env` and adjust:

- `PORT` — port to listen on (default `4000`).
- `ERROR_RATE` — 0-100, percent chance a request fails with a simulated `500` before your
  route logic runs.
- `MIN_DELAY_MS` — artificial latency (ms) added to every request.

## Auth

Protected routes require `Authorization: Bearer <accessToken>`, where `accessToken` comes
from `POST /api/auth/login`. Sessions are stored in memory and are cleared on restart.

Demo credentials (see `users.json`): `first` / `user`, `second` / `user`, `third` / `user`.

## Endpoints

All responses are JSON. Errors use a non-2xx status with body `{ "error": { "text": "..." } }`.
A simulated transport failure (governed by `ERROR_RATE`) returns `500` with the same shape.

| Method | Path                      | Auth | Description |
|--------|---------------------------|------|-------------|
| GET    | `/api/events`             | no   | All events, or events whose title contains `?q=` (case-insensitive) |
| GET    | `/api/events/:id`         | no   | A single event by id (`404` if not found) |
| GET    | `/api/tickets`            | no   | Tickets currently available for purchase, optionally filtered by `?q=` against the event title |
| POST   | `/api/auth/login`         | no   | Body `{ username, password }` → `{ accessToken }` |
| POST   | `/api/auth/logout`        | yes  | Invalidates the current access token |
| GET    | `/api/auth/me`            | yes  | The signed-in user, including `cart`, `ticketWallet`, `walletTicketCount`, `creditCardNumber` |
| GET    | `/api/cart`               | yes  | The signed-in user's current cart |
| POST   | `/api/cart/items`         | yes  | Body `{ ticketId }` → adds one unit to the cart and decrements availability |
| DELETE | `/api/cart/items/:ticketId` | yes | Removes one unit of that ticket from the cart and restores availability |
| POST   | `/api/checkout`           | yes  | Body `{ creditCardNumber }` → validates (Luhn), moves the cart into the wallet, clears the cart |
| PUT    | `/api/me/credit-card`     | yes  | Body `{ creditCardNumber }` → validates and saves the card on the user |

### Example flow

```bash
# 1) Log in
curl -s -X POST http://localhost:4000/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"first","password":"user"}'
# => { "accessToken": "..." }

TOKEN=<paste accessToken here>

# 2) Browse events / tickets
curl -s http://localhost:4000/api/events
curl -s "http://localhost:4000/api/tickets?q=jazz"

# 3) Add a ticket to the cart
curl -s -X POST http://localhost:4000/api/cart/items \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"ticketId":"tkt_01_01"}'

# 4) Check out
curl -s -X POST http://localhost:4000/api/checkout \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"creditCardNumber":"4111111111111111"}'

# 5) Read your wallet
curl -s http://localhost:4000/api/auth/me -H "Authorization: Bearer $TOKEN"
```

## Notes

- All state (sessions, carts, wallets, saved cards, ticket availability) is in-memory and
  resets on restart.
- Credit card numbers are only validated (length + Luhn) for a believable flow; storing raw
  PANs is for this demo only and is not how a real payment flow should work.
