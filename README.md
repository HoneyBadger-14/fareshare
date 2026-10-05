# FareShare Backend

Java 21 and Maven are required. The API uses Spring Boot 4, Spring Data JPA, Flyway, and PostgreSQL. A local profile uses a file-backed H2 database so the API can be developed before Neon connection details are available.

## Java package layout

| Package | Responsibility |
| --- | --- |
| `com.fareshare.model` | JPA entities and supported country catalog |
| `com.fareshare.controller` | REST endpoints, request/response records, and API errors |
| `com.fareshare.service` | Group, expense, balance, settlement, invitation, and identity logic |
| `com.fareshare.repository` | Spring Data JPA repository interfaces |
| `com.fareshare.config` | Local security configuration |

`FareShareApplication` stays in the parent package so Spring scans all subpackages.

## Run locally

From the `backend` directory:

```sh
mvn test
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

The local API listens on `127.0.0.1:8080`. Supply `X-Dev-Email` on each API request to simulate a signed-in user. The user is created on first use. This header is accepted only in the `local` profile. Local data is stored in `.local/` and ignored by Git.

```sh
curl -H 'X-Dev-Email: alice@example.com' http://127.0.0.1:8080/api/v1/me
curl -X POST http://127.0.0.1:8080/api/v1/groups \
  -H 'X-Dev-Email: alice@example.com' -H 'Content-Type: application/json' \
  -d '{"name":"Trip","currencyCode":"INR"}'
```

The group response includes the creator's `userId`. Use `GET /api/v1/me` for other local test users. Expense and settlement creation require an `Idempotency-Key` header. Money is sent as integer minor units, so `90000` INR means ₹900.00.

## Test with Postman

Import `postman/FareShare.local.postman_collection.json` into Postman. Start the API with the `local` profile, then run the collection's folders in numeric order. The requests create three local users and save their user IDs, group ID, invitation tokens, expense IDs, and settlement IDs as collection variables. The default `baseUrl` is `http://127.0.0.1:8080`.

Invitation tokens come from the local `localJoinLink` response; no email is sent. Expense edits, reversals, invite acceptance, and settlement decisions are state-changing actions that may return a conflict if you run them again. To create an additional expense or settlement in the same group, change its idempotency-key collection variable.

## Time and date convention

All event timestamps (`createdAt`, `updatedAt`, `joinedAt`, `decidedAt`, `acceptedAt`, `expiresAt`, and error `timestamp`) represent UTC instants. The database columns use `timestamp with time zone`; Hibernate uses UTC for JDBC, and the API emits ISO 8601 values with a `Z` suffix, such as `2026-10-04T10:30:00Z`. Store and compare these as instants. The Flutter client should convert them using the device's IANA time zone and format them using the user's locale. A country or language alone cannot identify a time zone.

`spentOn` is a date-only value (`YYYY-MM-DD`) chosen for an expense. It is not a timestamp and must not be shifted between time zones.

## API implemented

| Area | Endpoints |
| --- | --- |
| Identity for local testing | `GET /api/v1/me` |
| Country and currency choices | `GET /api/v1/catalog/countries` |
| Groups | `GET, POST /api/v1/groups`; `GET /api/v1/groups/{groupId}` |
| Invitations | `POST /api/v1/groups/{groupId}/invites`; `POST /api/v1/invites/accept` |
| Expenses | `GET, POST /api/v1/groups/{groupId}/expenses`; `PATCH /api/v1/groups/{groupId}/expenses/{expenseId}`; `POST /api/v1/groups/{groupId}/expenses/{expenseId}/reverse` |
| Balances and activity | `GET /api/v1/groups/{groupId}/balances`; `GET /api/v1/groups/{groupId}/activity` |
| Settlements | `GET, POST /api/v1/groups/{groupId}/settlements`; `POST /api/v1/settlements/{settlementId}/confirm` or `/reject` |

For an expense, set `splitMethod` to `EQUAL` and supply `participantIds`, or set it to `EXACT` and supply `shares` as objects with `userId` and `amountMinor`. Only the expense creator can edit or reverse it. An edit must supply the last `version` as `expectedVersion`. Only the payment recipient can confirm or reject a pending settlement. Pending settlements do not change balances.

Local invitation creation returns a `localJoinLink` with the token so the acceptance flow can be tested. It does **not** send email. Production invitation delivery remains unavailable until an email provider is configured.

## Neon connection later

The default profile expects these environment variables when a PostgreSQL connection is ready:

```text
FARESHARE_DATABASE_URL=jdbc:postgresql://HOST:PORT/DATABASE?sslmode=require
FARESHARE_DATABASE_USER=...
FARESHARE_DATABASE_PASSWORD=...
```

Flyway applies migrations from `src/main/resources/db/migration`. The default profile does not yet have a validated identity provider or invitation email provider. Complete those integrations before exposing it to users. Database credentials and auth secrets belong in environment variables or a secret manager.
