import "dotenv/config";
import { randomBytes } from "node:crypto";
import { readFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";
import express from "express";

const __dirname = dirname(fileURLToPath(import.meta.url));
const dataPath = join(__dirname, "data.json");
const usersPath = join(__dirname, "users.json");
const venuesPath = join(__dirname, "venues.json");
const imagesDir = join(__dirname, "images");

const env = {
  errorRate: Math.min(100, Math.max(0, Number(process.env.ERROR_RATE ?? 0))),
  minDelayMs: Math.max(0, Math.floor(Number(process.env.MIN_DELAY_MS ?? 0))),
};

/** Simulated failure body; sent when ERROR_RATE triggers before a route handler runs. */
const MOCK_FAILURE = { text: "Request failed (simulated)." };

function shouldMockFail() {
  return Math.random() * 100 < env.errorRate;
}

async function beforeEveryOp() {
  if (env.minDelayMs > 0) {
    await new Promise((r) => setTimeout(r, env.minDelayMs));
  }
}

/** Deep clone of event/ticket data from file; updated in memory when tickets are added to cart. */
const catalog = structuredClone(JSON.parse(readFileSync(dataPath, "utf8")));
const userRecords = JSON.parse(readFileSync(usersPath, "utf8"));
const venuesData = JSON.parse(readFileSync(venuesPath, "utf8"));

/** @type {Map<string, {id: string, name: string}>} venue id -> venue record */
const venuesById = new Map(venuesData.venues.map((v) => [v.id, v]));

/** @type {Map<string, string>} accessToken -> username (demo only; not for production) */
const activeSessions = new Map();

/** @type {Map<string, Array<CartLineData>>} username -> cart lines */
const userCarts = new Map();

/** @type {Map<string, Array<OwnedTicketData>>} user id (e.g. usr_1) -> purchased tickets */
const userWallets = new Map();

// NOTE: storing raw PANs in memory is for this assignment only. In production a
// payment processor token (e.g. Stripe payment method id) would be stored instead.
/** @type {Map<string, string>} username -> saved credit card number */
const userCreditCards = new Map();

/**
 * @typedef {object} CartLineData
 * @property {string} ticketId
 * @property {string} eventId
 * @property {string} eventTitle
 * @property {string} ticketLabel
 * @property {number} priceCents
 * @property {string} currency
 */

/**
 * @typedef {object} OwnedTicketData
 * @property {string} id
 * @property {string} label
 * @property {number} priceCents
 * @property {string} currency
 * @property {string} barcode EAN-13 (13 digits, valid check digit)
 */

/**
 * GS1 GTIN-13 check digit: positions 1-12 from the left, multiply by 1,3,1,3,...
 * (GS1: odd positions from the left x1, even positions x3).
 * @param {string} twelveDigits
 * @returns {number} 0-9
 */
function ean13CheckDigitFrom12(twelveDigits) {
  if (twelveDigits.length !== 12) {
    throw new Error("EAN-13 check needs exactly 12 data digits.");
  }
  let sum = 0;
  for (let i = 0; i < 12; i += 1) {
    const w = i % 2 === 0 ? 1 : 3;
    sum += Number(twelveDigits[i]) * w;
  }
  return (10 - (sum % 10)) % 10;
}

/**
 * @param {string} s
 * @returns {boolean}
 */
function isValidEan13(s) {
  if (!/^\d{13}$/.test(s)) {
    return false;
  }
  return ean13CheckDigitFrom12(s.slice(0, 12)) === Number(s[12]);
}

/** Returns a 13-digit EAN-13 with a correct GS1 mod-10 check digit (verifiable with scanners/validators). */
function generateEan13Barcode() {
  let digits = "";
  for (let i = 0; i < 12; i += 1) {
    digits += Math.floor(Math.random() * 10);
  }
  const check = ean13CheckDigitFrom12(digits);
  const full = `${digits}${check}`;
  if (!isValidEan13(full)) {
    throw new Error("EAN-13 internal check failed.");
  }
  return full;
}

function findUserByUsername(username) {
  return userRecords.users.find((u) => u.username === username) ?? null;
}

function toPublicUser(row) {
  if (!row) {
    return null;
  }
  return {
    id: row.id,
    firstName: row.firstName,
    lastName: row.lastName,
    age: row.age,
    username: row.username,
  };
}

/** Resolve a bearer access token to a public user, or an [Error] shape. */
function getSessionWithError(accessToken) {
  if (!accessToken || !String(accessToken).trim()) {
    return { user: null, error: { text: "Missing or invalid access token." } };
  }
  const username = activeSessions.get(accessToken.trim());
  if (!username) {
    return { user: null, error: { text: "Invalid or unknown access token." } };
  }
  const row = findUserByUsername(username);
  if (!row) {
    return { user: null, error: { text: "Session is corrupted." } };
  }
  return { user: toPublicUser(row), error: null };
}

/**
 * Find a ticket in the in-memory catalog.
 * @returns {{ event: object, ticket: object } | null}
 */
function findTicketInCatalog(ticketId) {
  for (const event of catalog.events) {
    const t = (event.tickets ?? []).find((x) => x.id === ticketId);
    if (t) {
      return { event, ticket: t };
    }
  }
  return null;
}

function getOrCreateCart(username) {
  if (!userCarts.has(username)) {
    userCarts.set(username, []);
  }
  return userCarts.get(username);
}

function serializeCart(username) {
  const lines = getOrCreateCart(username);
  return { lines: lines.map((line) => ({ ...line })) };
}

function luhnCheck(digits) {
  let sum = 0;
  let alt = false;
  for (let i = digits.length - 1; i >= 0; i -= 1) {
    let n = Number(digits[i]);
    if (Number.isNaN(n)) {
      return false;
    }
    if (alt) {
      n *= 2;
      if (n > 9) {
        n -= 9;
      }
    }
    sum += n;
    alt = !alt;
  }
  return sum % 10 === 0;
}

function isPlausibleCardNumber(raw) {
  const digits = String(raw ?? "").replace(/\D/g, "");
  if (digits.length < 12 || digits.length > 19) {
    return false;
  }
  return luhnCheck(digits);
}

function ticketsAvailableForPurchase(tickets) {
  return (tickets ?? []).filter((t) => t.quantityAvailable > 0);
}

function searchEventsByTitle(query) {
  const needle = query.trim();
  if (!needle) {
    return [];
  }
  const lower = needle.toLowerCase();
  return catalog.events.filter((e) => e.title.toLowerCase().includes(lower));
}

/** Build an absolute URL for a static image, using the incoming request's host so it works behind any port/proxy. */
function buildImageUrl(req, imageFile) {
  const host = req.headers["host"] ?? `localhost:${process.env.PORT ?? 4000}`;
  const proto = req.headers["x-forwarded-proto"] ?? req.protocol ?? "http";
  return `${proto}://${host}/images/${imageFile}`;
}

function serializeVenue(req, venue) {
  if (!venue) {
    return null;
  }
  return {
    id: venue.id,
    name: venue.name,
    imageUrl: buildImageUrl(req, venue.imageFile),
  };
}

function serializeTicket(ticket) {
  return {
    id: ticket.id,
    label: ticket.label,
    priceCents: ticket.priceCents,
    currency: ticket.currency,
    quantityAvailable: ticket.quantityAvailable,
  };
}

function serializeEvent(req, event) {
  return {
    id: event.id,
    title: event.title,
    description: event.description ?? null,
    startsAt: event.startsAt,
    venue: serializeVenue(req, venuesById.get(event.venueId) ?? null),
    tickets: (event.tickets ?? []).map(serializeTicket),
    imageUrl: buildImageUrl(req, event.imageFile),
  };
}

function serializeUser(row) {
  const wallet = userWallets.get(row.id) ?? [];
  return {
    id: row.id,
    firstName: row.firstName,
    lastName: row.lastName,
    age: row.age,
    username: row.username,
    cart: serializeCart(row.username).lines,
    walletTicketCount: wallet.length,
    ticketWallet: wallet.map((item) => ({ ...item })),
    creditCardNumber: userCreditCards.get(row.username) ?? null,
  };
}

/** Extracts the bearer token from `Authorization: Bearer <token>`. */
function getBearerToken(req) {
  const header = req.headers["authorization"];
  if (!header) {
    return null;
  }
  const match = /^Bearer\s+(.+)$/i.exec(header.trim());
  return match ? match[1].trim() : null;
}

/** Requires a valid `Authorization: Bearer <token>` header; attaches `req.session` on success. */
function requireAuth(req, res, next) {
  const accessToken = getBearerToken(req);
  const session = getSessionWithError(accessToken);
  if (session.error) {
    return res.status(401).json({ error: session.error });
  }
  req.accessToken = accessToken;
  req.session = session;
  next();
}

/** Applies the simulated latency/error-rate to every /api route, mirroring the GraphQL backend's mock behavior. */
async function mockBehavior(_req, res, next) {
  await beforeEveryOp();
  if (shouldMockFail()) {
    return res.status(500).json({ error: MOCK_FAILURE });
  }
  next();
}

const app = express();
app.use(express.json());
app.use("/images", express.static(imagesDir, { maxAge: "1d" }));

const api = express.Router();
api.use(mockBehavior);

/**
 * GET /api/events?q=<text>
 * All events, or events whose title contains `q` (case-insensitive) when provided.
 */
api.get("/events", (req, res) => {
  const q = typeof req.query.q === "string" ? req.query.q : "";
  const events = q.trim() ? searchEventsByTitle(q) : catalog.events;
  res.json({ events: events.map((e) => serializeEvent(req, e)) });
});

/** GET /api/events/:id — a single event by id. */
api.get("/events/:id", (req, res) => {
  const event = catalog.events.find((e) => e.id === req.params.id) ?? null;
  if (!event) {
    return res.status(404).json({ error: { text: "Event not found." } });
  }
  res.json({ event: serializeEvent(req, event) });
});

/**
 * GET /api/tickets?q=<text>
 * Tickets currently available for purchase (quantityAvailable > 0), with parent event info.
 * When `q` is omitted or blank, all such tickets are returned; otherwise only events whose
 * title contains the text (case-insensitive) are included.
 */
api.get("/tickets", (req, res) => {
  const raw = typeof req.query.q === "string" ? req.query.q : "";
  const needle = raw.trim();
  const out = [];
  for (const ev of catalog.events) {
    if (needle && !ev.title.toLowerCase().includes(needle.toLowerCase())) {
      continue;
    }
    for (const t of ticketsAvailableForPurchase(ev.tickets)) {
      out.push({
        eventId: ev.id,
        eventTitle: ev.title,
        ticket: serializeTicket(t),
      });
    }
  }
  res.json({ tickets: out });
});

/** POST /api/auth/login { username, password } */
api.post("/auth/login", (req, res) => {
  const { username, password } = req.body ?? {};
  const user = findUserByUsername(username);
  if (!user || user.password !== password) {
    return res.status(401).json({ error: { text: "Invalid username or password." } });
  }
  const accessToken = randomBytes(32).toString("hex");
  activeSessions.set(accessToken, user.username);
  res.json({ accessToken });
});

/** POST /api/auth/logout — requires Authorization: Bearer <accessToken>. */
api.post("/auth/logout", requireAuth, (req, res) => {
  activeSessions.delete(req.accessToken);
  res.json({ ok: true });
});

/** GET /api/auth/me — the signed-in user, including cart and wallet. */
api.get("/auth/me", requireAuth, (req, res) => {
  const row = findUserByUsername(req.session.user.username);
  res.json({ user: serializeUser(row) });
});

/** GET /api/cart — the signed-in user's current cart. */
api.get("/cart", requireAuth, (req, res) => {
  res.json({ cart: serializeCart(req.session.user.username) });
});

/**
 * POST /api/cart/items { ticketId }
 * Adds one unit of a ticket to the signed-in user's cart and decrements in-memory availability.
 */
api.post("/cart/items", requireAuth, (req, res) => {
  const { ticketId } = req.body ?? {};
  const username = req.session.user.username;
  const found = findTicketInCatalog(ticketId);
  if (!found) {
    return res.status(404).json({ error: { text: "Unknown ticket id." }, cart: serializeCart(username) });
  }
  const { event, ticket } = found;
  if (ticket.quantityAvailable <= 0) {
    return res.status(409).json({ error: { text: "This ticket is sold out." }, cart: serializeCart(username) });
  }
  ticket.quantityAvailable -= 1;
  const line = {
    ticketId: ticket.id,
    eventId: event.id,
    eventTitle: event.title,
    ticketLabel: ticket.label,
    priceCents: ticket.priceCents,
    currency: ticket.currency,
  };
  getOrCreateCart(username).push(line);
  res.status(201).json({ cart: serializeCart(username) });
});

/**
 * DELETE /api/cart/items/:ticketId
 * Removes one unit of a ticket from the cart (one line per call) and restores availability.
 */
api.delete("/cart/items/:ticketId", requireAuth, (req, res) => {
  const { ticketId } = req.params;
  const username = req.session.user.username;
  const found = findTicketInCatalog(ticketId);
  if (!found) {
    return res.status(404).json({ error: { text: "Unknown ticket id." }, cart: serializeCart(username) });
  }
  const cart = getOrCreateCart(username);
  const index = cart.findIndex((line) => line.ticketId === ticketId);
  if (index === -1) {
    return res.status(404).json({ error: { text: "This ticket is not in your cart." }, cart: serializeCart(username) });
  }
  cart.splice(index, 1);
  found.ticket.quantityAvailable += 1;
  res.json({ cart: serializeCart(username) });
});

/**
 * POST /api/checkout { creditCardNumber }
 * Pays with a card number, moves the current cart into the wallet, and clears the cart.
 * The card is validated (length and Luhn) for a believable flow; it is not stored.
 */
api.post("/checkout", requireAuth, (req, res) => {
  const { creditCardNumber } = req.body ?? {};
  if (!isPlausibleCardNumber(creditCardNumber)) {
    return res.status(400).json({ error: { text: "Invalid or unsupported credit card number." } });
  }
  const userId = req.session.user.id;
  const username = req.session.user.username;
  const cart = getOrCreateCart(username);
  if (cart.length === 0) {
    return res.status(400).json({ error: { text: "Cart is empty." } });
  }
  if (!userWallets.has(userId)) {
    userWallets.set(userId, []);
  }
  const wallet = userWallets.get(userId);
  const added = [];
  for (const line of cart) {
    const item = {
      id: `wlt_${randomBytes(9).toString("hex")}`,
      label: line.ticketLabel,
      priceCents: line.priceCents,
      currency: line.currency,
      barcode: generateEan13Barcode(),
    };
    wallet.push(item);
    added.push({ ...item });
  }
  cart.length = 0;
  res.json({ purchasedTickets: added });
});

/**
 * PUT /api/me/credit-card { creditCardNumber }
 * Saves a credit card number on the signed-in user so they can skip re-entering it at checkout.
 */
api.put("/me/credit-card", requireAuth, (req, res) => {
  const { creditCardNumber } = req.body ?? {};
  if (!isPlausibleCardNumber(creditCardNumber)) {
    return res.status(400).json({ error: { text: "Invalid or unsupported credit card number." } });
  }
  userCreditCards.set(req.session.user.username, String(creditCardNumber));
  const row = findUserByUsername(req.session.user.username);
  res.json({ user: serializeUser(row) });
});

app.use("/api", api);

app.use((_req, res) => {
  res.status(404).json({ error: { text: "Not found." } });
});

const port = process.env.PORT ? Number(process.env.PORT) : 4000;

app.listen(port, () => {
  console.log(
    `Server ready at http://localhost:${port}/api — ERROR_RATE=${env.errorRate} MIN_DELAY_MS=${env.minDelayMs}`,
  );
});
