# TicketWise

[![CI](https://github.com/HAIZ4D/ticketwise/actions/workflows/ci.yml/badge.svg)](https://github.com/HAIZ4D/ticketwise/actions/workflows/ci.yml)

An IT helpdesk built with Java and Spring Boot. Employees report IT problems in plain words, AI suggests a category and priority for each ticket, and IT agents work through them with a clear status workflow.

**Status: backend, React dashboard, tests, Docker and CI complete (Phase 4).**

## What it does

- **React dashboard:** a ticket list with filters, ticket pages with comments and history, a create form, and stats and user screens for admins.
- **Accounts and roles:** log in with a JWT. Employees see only their own tickets, while agents and admins manage all of them.
- **Tickets:** create, list, view, assign and change status. Invalid status moves are rejected.
- **AI classification:** Gemini suggests a category, priority and a one-line reason for each new ticket.
- **Rule-based fallback:** if Gemini is down, returns a bad answer or has no API key, keyword rules take over, so a ticket is never lost.
- **Agent override:** an agent can change the category or priority. The AI suggestion and the final value are both kept.
- **Comments and audit history:** every status, category, priority and assignee change records who changed what, and when, taken from the login.
- **Filters and stats:** filter by status, priority, category and assignee, with paging, sorting and summary counts.

## Run it

### With Docker

This starts the dashboard, the API and PostgreSQL with one command. You only need Docker.

```bash
docker compose up --build
```

| What | Where |
| --- | --- |
| Dashboard | http://localhost:3000 |
| Swagger UI for the API | http://localhost:8080/swagger-ui.html |

Data is kept in a Docker volume, so it survives restarts. `docker compose down -v` deletes it. Settings in a `.env` file are optional, but you need `ADMIN_EMAIL` and `ADMIN_PASSWORD` to get an admin account.

### Without Docker

**Backend:** you need JDK 25. You don't need to install Maven, because the project includes Maven Wrapper.

```bash
# Windows
.\mvnw.cmd spring-boot:run

# macOS or Linux
./mvnw spring-boot:run
```

The default `dev` profile uses an in-memory H2 database, so nothing else needs to be installed. Data is lost when the app stops.

**Frontend:** you need Node 22 or newer. Vite forwards `/api` to the backend on port 8080.

```bash
cd frontend
npm install
npm run dev
```

Then open http://localhost:5173.

## API

All paths start with `/api/v1`. Everything except `/auth` needs an `Authorization: Bearer <token>` header. In Swagger UI, use the **Authorize** button.

| Method | Path | What it does | Who |
| --- | --- | --- | --- |
| POST | `/auth/register` | Create an account and get a token | Anyone |
| POST | `/auth/login` | Get a token | Anyone |
| POST | `/tickets` | Create a ticket and classify it | Everyone |
| GET | `/tickets` | List tickets. Filters: `status`, `priority`, `category`, `assigneeId`. Also `page`, `size`, `sort` | Employees get their own |
| GET | `/tickets/{id}` | Get one ticket | Employees get their own |
| GET | `/tickets/{id}/comments` | List comments | Employees get their own |
| POST | `/tickets/{id}/comments` | Add a comment | Employees on their own |
| GET | `/tickets/{id}/history` | Read the audit log | Employees get their own |
| PATCH | `/tickets/{id}/status` | Change status | Agent, Admin |
| PATCH | `/tickets/{id}/classification` | Override category and/or priority | Agent, Admin |
| PATCH | `/tickets/{id}/assignee` | Assign to an agent, or `null` to unassign | Agent, Admin |
| POST | `/tickets/{id}/reclassify` | Run the classifier again | Agent, Admin |
| GET | `/users/agents` | List agents and admins | Agent, Admin |
| GET | `/users` | List users | Admin |
| PATCH | `/users/{id}/role` | Change a user's role | Admin |
| GET | `/stats` | Counts by status, category and priority | Admin |

Register, then create a ticket:

```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"aina@example.com","password":"choose-a-password"}'

curl -X POST http://localhost:8080/api/v1/tickets \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token from the response above>" \
  -d '{"title":"VPN keeps disconnecting","description":"My VPN drops every 10 minutes."}'
```

Everyone who registers is an Employee. To get agents, set `ADMIN_EMAIL` and `ADMIN_PASSWORD` so the app creates the first admin at startup, then log in as that admin and use `PATCH /users/{id}/role`.

## Status workflow

| From | Allowed next status |
| --- | --- |
| OPEN | IN_PROGRESS, CLOSED |
| IN_PROGRESS | RESOLVED, OPEN |
| RESOLVED | CLOSED, IN_PROGRESS |
| CLOSED | none |

## Run the tests

```bash
# Fast backend tests, no Docker needed
./mvnw test

# All backend tests, including real PostgreSQL. Docker must be running.
./mvnw verify

# Frontend tests, lint and type checks
cd frontend
npm test
npm run lint
npm run typecheck
```

On Windows, use `.\mvnw.cmd` instead of `./mvnw`. Tests never call the real Gemini API.

Every push to `main` runs all of this and builds both Docker images with GitHub Actions.

## Configuration

Settings come from environment variables or a local `.env` file. Copy `.env.example` to `.env` and fill in your own values. Never commit `.env`.

To use Gemini, set `GEMINI_API_KEY`. Without it, the app uses the keyword rules only.

Set `JWT_SECRET` to at least 32 random characters. Without it, the app makes a random key at startup, so everyone has to log in again after a restart.

## Security

- Passwords are stored only as BCrypt hashes.
- The role is read from the database on every request, so a role change takes effect immediately.
- An employee asking for someone else's ticket gets the same 404 as for a ticket that doesn't exist.
- The dashboard hides staff controls from employees, but the rules are enforced by the API, not the screen.
- The dashboard is served with a Content Security Policy, and ticket text is always shown as plain text.

The dashboard keeps its token in `localStorage`, so it survives a refresh. An `httpOnly` cookie would be safer against script attacks, but would need CSRF protection.

Before any real deployment, these are still missing: a limit on login attempts, logout and refresh tokens, and email verification. Without email verification, anyone can register with any address. A token stays valid until it expires, 60 minutes by default.
