# TicketWise

[![CI](https://github.com/HAIZ4D/ticketwise/actions/workflows/ci.yml/badge.svg)](https://github.com/HAIZ4D/ticketwise/actions/workflows/ci.yml)

An IT helpdesk built with Java and Spring Boot. Employees report IT problems in plain words, AI suggests a category and priority for each ticket, and IT agents work through them with a clear status workflow.

**Status: backend with login and roles, tests, Docker and CI complete (Phase 3).** The React dashboard is not built yet.

## What it does

- **Accounts and roles:** log in with a JWT. Employees see only their own tickets, while agents and admins manage all of them.
- **Tickets:** create, list, view, assign and change status. Invalid status moves are rejected.
- **AI classification:** Gemini suggests a category, priority and a one-line reason for each new ticket.
- **Rule-based fallback:** if Gemini is down, returns a bad answer or has no API key, keyword rules take over, so a ticket is never lost.
- **Agent override:** an agent can change the category or priority. The AI suggestion and the final value are both kept.
- **Comments and audit history:** every status, category, priority and assignee change records who changed what, and when, taken from the login.
- **Filters and stats:** filter by status, priority, category and assignee, with paging, sorting and summary counts.

## Run it

### With Docker

This starts the app and PostgreSQL with one command. You only need Docker.

```bash
docker compose up --build
```

Then open Swagger UI to try the API: http://localhost:8080/swagger-ui.html

Data is kept in a Docker volume, so it survives restarts. `docker compose down -v` deletes it. Settings in a `.env` file are optional here.

### Without Docker

You need JDK 25. You don't need to install Maven, because the project includes Maven Wrapper.

```bash
# Windows
.\mvnw.cmd spring-boot:run

# macOS or Linux
./mvnw spring-boot:run
```

The default `dev` profile uses an in-memory H2 database, so nothing else needs to be installed. Data is lost when the app stops.

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
# Fast tests, no Docker needed
./mvnw test

# All tests, including one on real PostgreSQL. Docker must be running.
./mvnw verify
```

On Windows, use `.\mvnw.cmd` instead of `./mvnw`. Tests never call the real Gemini API.

Every push to `main` runs all the tests and builds the Docker image with GitHub Actions.

## Configuration

Settings come from environment variables or a local `.env` file. Copy `.env.example` to `.env` and fill in your own values. Never commit `.env`.

To use Gemini, set `GEMINI_API_KEY`. Without it, the app uses the keyword rules only.

Set `JWT_SECRET` to at least 32 random characters. Without it, the app makes a random key at startup, so everyone has to log in again after a restart.

## Security

- Passwords are stored only as BCrypt hashes.
- The role is read from the database on every request, so a role change takes effect immediately.
- An employee asking for someone else's ticket gets the same 404 as for a ticket that doesn't exist.

Before any real deployment, these are still missing: a limit on login attempts, logout and refresh tokens, and email verification. Without email verification, anyone can register with any address. A token stays valid until it expires, 60 minutes by default.
