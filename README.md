# TicketWise

An IT helpdesk built with Java and Spring Boot. Employees report IT problems in plain words, AI suggests a category and priority for each ticket, and IT agents work through them with a clear status workflow.

**Status: core backend complete (Phase 1).** Automated tests, Docker, login and the React dashboard are not built yet.

## What it does

- **Tickets:** create, list, view and change status. Invalid status moves are rejected.
- **AI classification:** Gemini suggests a category, priority and a one-line reason for each new ticket.
- **Rule-based fallback:** if Gemini is down, returns a bad answer or has no API key, keyword rules take over, so a ticket is never lost.
- **Agent override:** an agent can change the category or priority. The AI suggestion and the final value are both kept.
- **Comments and audit history:** every status, category and priority change records who changed what, and when.
- **Filters and stats:** filter by status, priority and category, with paging, sorting and summary counts.

## Run it

You need JDK 25. You don't need to install Maven, because the project includes Maven Wrapper.

```bash
# Windows
.\mvnw.cmd spring-boot:run

# macOS or Linux
./mvnw spring-boot:run
```

Then open Swagger UI to try the API: http://localhost:8080/swagger-ui.html

The default `dev` profile uses an in-memory H2 database, so nothing else needs to be installed. Data is lost when the app stops.

## API

All paths start with `/api/v1`.

| Method | Path | What it does |
| --- | --- | --- |
| POST | `/tickets` | Create a ticket and classify it |
| GET | `/tickets` | List tickets. Filters: `status`, `priority`, `category`. Also `page`, `size`, `sort` |
| GET | `/tickets/{id}` | Get one ticket |
| PATCH | `/tickets/{id}/status` | Change status |
| PATCH | `/tickets/{id}/classification` | Override category and/or priority |
| POST | `/tickets/{id}/reclassify` | Run the classifier again |
| GET | `/tickets/{id}/comments` | List comments |
| POST | `/tickets/{id}/comments` | Add a comment |
| GET | `/tickets/{id}/history` | Read the audit log |
| GET | `/stats` | Counts by status, category and priority |

Create a ticket:

```bash
curl -X POST http://localhost:8080/api/v1/tickets \
  -H "Content-Type: application/json" \
  -d '{"title":"VPN keeps disconnecting","description":"My VPN drops every 10 minutes.","requesterEmail":"aina@example.com"}'
```

## Status workflow

| From | Allowed next status |
| --- | --- |
| OPEN | IN_PROGRESS, CLOSED |
| IN_PROGRESS | RESOLVED, OPEN |
| RESOLVED | CLOSED, IN_PROGRESS |
| CLOSED | none |

## Run the tests

```bash
# Windows
.\mvnw.cmd test

# macOS or Linux
./mvnw test
```

## Configuration

Settings come from environment variables or a local `.env` file. Copy `.env.example` to `.env` and fill in your own values. Never commit `.env`.

To use Gemini, set `GEMINI_API_KEY`. Without it, the app uses the keyword rules only.

## Security

The API has no login yet. Until login is added, requests use an email field instead of a logged-in user, and anyone can send any email. Don't expose the app to the internet.
