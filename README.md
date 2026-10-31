# TicketWise

An IT helpdesk built with Java and Spring Boot. Employees report IT problems in plain words, AI suggests a category and priority for each ticket, and IT agents work through them in a queue.

**Status: project setup (Phase 0).** The app starts and reports its health. Ticket features are not built yet.

## Run it

You need JDK 25. You don't need to install Maven, because the project includes Maven Wrapper.

```bash
# Windows
.\mvnw.cmd spring-boot:run

# macOS or Linux
./mvnw spring-boot:run
```

Check that it's running:

```bash
curl http://localhost:8080/actuator/health
```

The response includes `"status":"UP"`.

The default `dev` profile uses an in-memory H2 database, so nothing else needs to be installed. Data is lost when the app stops.

## Run the tests

```bash
# Windows
.\mvnw.cmd test

# macOS or Linux
./mvnw test
```

## Configuration

Settings come from environment variables or a local `.env` file. Copy `.env.example` to `.env` and fill in your own values. Never commit `.env`.

## Security

The API has no login until Phase 3. Until then, requests use an email field instead of a logged-in user, so don't expose the app to the internet.
