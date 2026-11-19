# phorest-techtest-thilagavathisanthoshkumar

Fruit Machine Service: a Spring Boot REST API that configures fruit machines (slots, colours, float, free plays), plays them, and refills them. Data is stored in MySQL.

## 1. Prerequisites

- JDK 21 (the bundled formatter fails on newer JDKs)
- Docker, running (tests and the dev launcher start MySQL through Testcontainers)
- Optional: Newman for the command-line Postman run (`npm i -g newman`)

## 2. One-time setup

```
git clone <repo-url>
cd phorest-techtest-thilagavathisanthoshkumar
git config core.hooksPath .githooks   # pre-commit formatter hook
./mvnw verify                          # build + all tests + format check
```

## 3. Run the API

### Option 0: Docker Compose (MySQL and app, one command)

```
docker compose --env-file .env.example up -d --build   # local only: uses the example passwords
curl http://localhost:8080/actuator/health
docker compose down -v      # stop and delete the data
```

#### Check metrics (once the app is UP)

When `/actuator/health` returns `UP`, check the metrics:

```
curl http://localhost:8080/actuator/metrics/fruitmachine.plays          # Actuator JSON
curl http://localhost:8080/actuator/prometheus | grep fruitmachine_      # Prometheus format
```

Make a paid play (Postman, or `POST /api/v1/machines/{id}/plays`), then open Grafana at http://localhost:3000 (login `admin`, password `GRAFANA_ADMIN_PASSWORD` from `.env`) and open the "Fruit Machine Service" dashboard. The graphs refresh every 10 seconds and show plays per second, cumulative payout and free plays. Prometheus is at http://localhost:9090 and scrapes the app's `/actuator/prometheus`.

Compose starts MySQL 8.0 and the app. The app connects as `fruitmachine_app`, which has rights on the `fruitmachine` schema only. Flyway still needs DDL rights on that schema to migrate at startup, so a separate migration user would be the next step if that matters. `.env` is gitignored; never commit it.

### Option A: dev launcher with its own MySQL (no setup)

Run `src/test/java/com/thilaga/fruitmachine/TestFruitmachineApplication.java` from your IDE (its `main` method). Docker must be running. The launcher starts a throwaway MySQL container. Data is lost when it stops.

### Option B: your own MySQL

Create an empty database, then point the app at it with environment variables (no datasource is set in `application.properties`):

```
export SPRING_DATASOURCE_URL='jdbc:mysql://localhost:3306/fruitmachine'
export SPRING_DATASOURCE_USERNAME=<user>
export SPRING_DATASOURCE_PASSWORD=<password>
./mvnw spring-boot:run
```

Flyway creates the tables on first start.

The API listens on `http://localhost:8080`.

## 4. Swagger UI

With the app running:

- Swagger UI: http://localhost:8080/swagger-ui/index.html
- OpenAPI document (JSON): http://localhost:8080/v3/api-docs

Use "Try it out" in Swagger UI to call endpoints directly.

## 5. Endpoints

| Method | Path | Purpose |
|---|---|---|
| POST | `/api/v1/machines` | Configure a machine (201 + `Location`) |
| GET | `/api/v1/machines/{id}` | Read machine state |
| POST | `/api/v1/machines/{id}/plays` | Play (`{"useFreePlay": false}`) |
| POST | `/api/v1/machines/{id}/refills` | Operator refill (`{"amountPence": 500}`) |

### Why MIN_RESERVE and refill exist (D10, D11)

Under the default policy MIN_RESERVE a machine refuses plays while its float is below five times the play cost (409 `machine-needs-refill`). This means every prize can always be paid in full from the float. The machine returns to service only when an operator refills it (`POST .../refills`); each refill is recorded in the machine's history.

### Policy NONE keeps the original rules

Policy NONE accepts every play and applies the shortfall rule literally: when the float cannot pay a prize in full, the unpaid amount becomes free plays (shortfall divided by the play cost, rounded up). Choose it at configuration with `"floatPolicy": "NONE"`.

Errors use RFC 7807 problem details (`application/problem+json`). Types: `validation-error` (400), `machine-not-found` (404), `machine-needs-refill` (409), `no-free-plays` (409), `machine-busy` (503 with `Retry-After`).

## 6. Test with the Postman collection

The collection is at `postman/fruitmachine-api.postman_collection.json`. It creates its own data, so you do not load any test data. Each run creates new machines, and later folders reuse the ids saved by earlier ones.

### Option A: command line (Newman)

With the app running:

```
newman run postman/fruitmachine-api.postman_collection.json --env-var baseUrl=http://localhost:8080
```

### Option B: Postman app

1. File → Import → select `postman/fruitmachine-api.postman_collection.json`.
2. Check the collection variable `baseUrl` is `http://localhost:8080`.
3. Run the collection (or each folder) top to bottom. Folder order matters.

### Rules

- Run folders in order. A folder run alone fails because its variables are empty.
- Use a fresh app (Option A) or a fresh database for a clean run. Re-runs against the same database still pass, because each run creates new machines.
- Jackpot cases use a one-slot, one-colour machine, so the result is certain.

### Not covered over HTTP

- AC-25 and AC-27: free-play credit needs a float shortfall, which needs forced spins. Covered by system tests (Issue 7).
- 503 `machine-busy` and AC-32 (lock timeout): need concurrent load. Covered by Issue 5.

## 7. Run the tests

```
./mvnw verify                                          # everything
./mvnw test -Dtest=MachineReadApiTest                  # one class
./mvnw test -Dtest=MachineInteractionApiTest#someName  # one method
./mvnw fmt:format                                      # fix formatting
```

## 8. Project layout

Packages under `com.thilaga.fruitmachine`:

- `api`: REST controllers, request/response records, error mapping
- `application`: `MachineService`, one transaction per use case
- `domain`: pure Java game rules (no Spring or JPA)
- `infrastructure`: JPA entities, repositories, Flyway migrations, spin generator

Design docs are in `docs/`. The implementation plan for this API is `docs/plans/US-04-rest-api-plan.md`.
