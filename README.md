# Parking area creation and layout editor

Provides a JSON REST API for parking areas and spots, backed by an Angular layout editor.
The domain model includes `ParkingArea`, `ParkingSpot`, `SpotType`, and `SpotStatus`.

The 2D editor uses **Fabric.js** (https://fabricjs.com), an open-source canvas library.
Fabric handles selecting, moving, resizing and rotating items with handles.
Our code only creates the shapes, snaps them to the 0.5 m grid and reads their
position back when the admin saves.

## Backend architecture (microservices)

```
 Angular / Android / Postman                         Keycloak :8180 (realm "parking", OAuth2 tokens)
          │  http://localhost:8080/api/...
          ▼
   gateway-service :8080 ── routes ──┬─ /api/notifications/** → lb://notification-service (Eureka)
   (Spring Cloud Gateway)            ├─ /api/billing/**       → billing-service :3000 (static, Node/Express)
                                     ├─ /api/**               → lb://parking-service  (Eureka)
                                     └─ /<service-id>/**      → discovered dynamically in Eureka

notification-service :8082 ──Feign + Resilience4j──▶ parking-service :8081 (backend/) ──▶ PostgreSQL (Supabase)
          ▲                                                 │  └──RestClient──▶ Open-Meteo (external)
          └───────── Kafka topic "reservation-events" ◀────┘

config-server :8888 (configuration of every service)   eureka-server :8761 (registry)
Prometheus :9090 (metrics) + Tempo :3200 (traces) ──▶ Grafana :3001 (dashboard "Parking - Microservices")
```

| Rubric item | Where | Postman folder |
|---|---|---|
| REST Spring Boot (JSON) | `backend/` controllers | 2 |
| REST with another framework | `billing-service/` (Node.js / Express, tests: `npm test`) | 3 |
| Registry (Eureka) | `eureka-server/`; clients: parking, notification, gateway | 4 |
| Config server | `config-server/` (files in `src/main/resources/config/`), `GET /api/pricing`, `/actuator/refresh` | 5 |
| Gateway | `gateway-service/src/main/resources/application.yml`: static, `lb://` and discovery routes | 6 |
| Circuit breaker | `notification-service/.../client/ResilientParkingClient.java` (circuit breaker, retry, fallback) | 9 |
| Synchronous communication | `notification-service/.../client/ParkingServiceClient.java` (OpenFeign) | 8 |
| Asynchronous communication | `backend/.../event/ReservationEventPublisher.java` → `notification-service/.../kafka/ReservationEventListener.java` | 8 |
| External service | `backend/.../external/WeatherClient.java` (RestClient → Open-Meteo), `GET /api/parking-areas/{id}/weather` | 7 |
| Prometheus, Tempo, Grafana | `infra/prometheus`, `infra/tempo`, `infra/grafana` (datasources + dashboard provisioned) | 10 |
| Keycloak | `infra/keycloak/parking-realm.json`: realm, roles, clients, users (imported on start) | 0 |
| Security OAuth2 | `backend/.../config/SecurityConfig.java` + `KeycloakJwtConverter.java`, `notification-service/.../config/SecurityConfig.java` | 1 |
| Docker deployment | `*/Dockerfile`, `docker-compose.yml` | all |

### Keycloak

Realm `parking`, imported from `infra/keycloak/parking-realm.json` on the first start:

- Roles: `ADMIN`, `USER`, `TICKET_COUNTER`.
- Clients: `parking-postman` (public, password grant and authorization code) and `parking-backend`
  (confidential, client credentials, secret `parking-backend-secret`).
- Users: `admin` / `admin123` (ADMIN), `counter` / `counter123` (TICKET_COUNTER), `driver` / `driver123` (USER).
  Change these passwords outside a demo.

Get a token: `POST http://localhost:8180/realms/parking/protocol/openid-connect/token` with
`grant_type=password&client_id=parking-postman&username=driver&password=driver123`, then send
`Authorization: Bearer <access_token>`.

The parking service accepts both the existing session cookie (Angular, Android) and Keycloak tokens. A token is
linked to the local account with the same e-mail (created on the first call), and its realm roles decide access.

### Run everything with Docker

```powershell
copy .env.example .env    # then set DB_PASSWORD in .env
docker compose up --build -d
docker compose ps         # wait until every service is running (about 2 minutes)
```

| URL | |
|---|---|
| http://localhost:8080 | Gateway (API) |
| http://localhost:8761 | Eureka dashboard |
| http://localhost:8888/parking-service/default | Config server |
| http://localhost:8180 | Keycloak (console: admin / admin) |
| http://localhost:3001 | Grafana (admin / admin), dashboard "Parking - Microservices" |
| http://localhost:9090 | Prometheus |

Circuit breaker demo: `docker compose stop parking-service`, call
`GET /api/notifications/reservations/1` a few times (fallback answer with `"fallback": true`), check
`GET /notification-service/actuator/circuitbreakers` (state `OPEN`), then `docker compose start parking-service`:
after 15 s the circuit goes half-open and closes again after 3 successful calls.

Config refresh demo: edit `config-server/src/main/resources/config/parking-service.yml` (mounted in the
container), then `POST /parking-service/actuator/refresh` with an admin token and `GET /api/pricing`.

### Run without Docker

Start the infrastructure with Docker (`docker compose up -d kafka keycloak tempo prometheus grafana`), then each
service in its own terminal (JDK 17, Maven, Node 18+), in this order:

```powershell
cd config-server; mvn spring-boot:run           # 8888
cd eureka-server; mvn spring-boot:run           # 8761
$env:DB_PASSWORD = "your-supabase-database-password"
cd backend; mvn spring-boot:run                 # parking-service, 8081
cd notification-service; mvn spring-boot:run    # 8082
cd billing-service; npm install; npm start      # 3000
cd gateway-service; mvn spring-boot:run         # 8080
```

`mvn package -DskipTests` at the repository root builds every Spring service.
If Kafka is down, reservations still work: the event is logged as not published.

### Tests with Postman

Import `postman/parking-microservices.postman_collection.json` and run it with the collection runner.
Folder 0 stores the Keycloak tokens used by the other folders. Set `spotId` to an available spot before folder 8.

## Run the backend alone

The Angular and Android clients call port 8080, which is now the gateway: run at least the gateway with the
backend (port 8081).

On first startup, the backend creates the initial administrator using `ADMIN_EMAIL` (default `admin@parking.com`) and `ADMIN_PASSWORD`. If `ADMIN_PASSWORD` is unset, it skips admin creation; public registration always creates a regular user.

The backend connects to the Supabase PostgreSQL pooler. The default URL and username point at this project's Supabase pooler; set `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` in the environment to use another connection or password. The schema is updated on startup and data is retained when the backend stops. Keep database credentials in environment variables, not in this file.

## Run the frontend

```
cd frontend
npm install
npm start
```

Open http://localhost:4200, create a parking area, then click "Edit layout".
`ng serve` forwards `/api` to the gateway on http://localhost:8080 (`frontend/proxy.conf.json`), so the
app calls a relative `/api` URL both locally and when deployed.

## Run the Android app

The Android emulator uses `http://10.0.2.2:8080/api/` to reach the backend on the host machine. For a physical device, set `api.url` in `android/local.properties` to the host computer's LAN address, for example `http://192.168.1.20:8080/api/`, and keep the phone and computer on the same network.

## Deploy on Railway

The repository root is only a Maven aggregator (it builds no jar). **Do not deploy the root.** Railway then
runs `java -jar target/*jar` and crashes with "Unable to access jarfile target/*jar". Create one Railway
service per folder from the same GitHub repo. For each service, in **Settings**:

- set **Root Directory** to the folder (e.g. `/backend`), so Railway builds that folder's `Dockerfile`;
- set **Config file path** to `/<folder>/railway.json` (health check and restart policy).

Railway sets `PORT`; every service listens on it. Use the service names below so the internal URLs match
(`<name>.railway.internal`).

| Railway service | Root Directory | Variables |
|---|---|---|
| `parking-service` | `/backend` | `DB_PASSWORD`, `ADMIN_PASSWORD`, `PORT=8081`, `EUREKA_ENABLED=false`, `KAFKA_ENABLED=false` (optional: `DB_URL`, `DB_USERNAME`) |
| `billing-service` | `/billing-service` | `PORT=3000` |
| `gateway-service` | `/gateway-service` | `PORT=8080`, `EUREKA_ENABLED=false`, `PARKING_SERVICE_URL=http://parking-service.railway.internal:8081`, `BILLING_SERVICE_URL=http://billing-service.railway.internal:3000` |
| `frontend` | `/frontend` | `API_URL=http://gateway-service.railway.internal:8080` |

Generate a public domain (Settings → Networking) for **`frontend` only**. nginx serves the Angular app and
forwards `/api` to the gateway, so the browser sees a single origin and the session and XSRF cookies work.
To give the Android app a public API, also generate a domain for `gateway-service` and set
`api.url=https://<gateway-domain>/api/` in `android/local.properties`.

Optional services (the core works without them): `notification-service` (`EUREKA_ENABLED=false`,
`PARKING_SERVICE_URL=...`, `KAFKA_BOOTSTRAP_SERVERS`), a Kafka broker (then drop `KAFKA_ENABLED=false` on the
parking service and set `KAFKA_BOOTSTRAP_SERVERS`), and Keycloak (`KEYCLOAK_ISSUER_URI`,
`KEYCLOAK_JWK_SET_URI`). The session login used by Angular and Android does not need Keycloak.

The Supabase session pooler accepts 15 clients in total. Each backend instance uses at most `DB_POOL_SIZE`
(default 5) connections, so a redeploy (old and new instance running together) stays under the limit.

## REST API

- `GET /api/parking-areas` — list parking areas.
- `POST /api/parking-areas` — create an area; returns `201 Created` with a `Location` header.
- `GET /api/parking-areas/{id}` — retrieve one area and its spots.
- `PUT /api/parking-areas/{id}/layout` — replace an area's layout and spot collection.
- `GET /api/parking-spots/{id}` — retrieve one spot.
- `PATCH /api/parking-spots/{id}` — update a spot's `type`, `status`, or both. For example, send `{"status":"NOT_AVAILABLE"}` to make a spot unavailable.

All endpoints return JSON. The admin endpoints require the administrator session created by `POST /api/auth/login`; the Angular app validates that session and sends the session cookie and CSRF token automatically. Reservations remain available to the Android client.

## Editor

- 1 square = 0.5 m, thick line every 5 m. All sizes are in metres.
- Drag items from the left onto the plan: 4 spot types, road, entrance, exit, pillar, wall, green space.
- Click an item: drag to move, corner handles to resize, top handle to rotate (snaps every 15°).
  Exact values can be typed in the right panel.
- Duplicate, create a row of N copies, delete, undo / redo.
- Levels A, B, C… (spots are numbered A-01, B-01…).
- Background: satellite view (Esri) from the latitude / longitude, or an uploaded image.
- Spots with a red border overlap another item.
- "Save layout" replaces the area's layout resource: spots are created, updated or removed
  in the database and `capacity` becomes the number of spots.

## Files

See [PROJECT-GUIDE.md](./PROJECT-GUIDE.md) for a file-by-file explanation of the backend and frontend.

```
frontend/src/app/
├── models.ts                          diagram types + layout format
├── parking.service.ts                 REST calls
├── parking-list/                      list + create page
└── layout-editor/
    ├── plan-items.ts                  turns an item into a Fabric shape and back
    ├── layout-editor.component.ts
    ├── layout-editor.component.html
    └── layout-editor.component.css
```

`ParkingArea.layout` stores the plan as JSON in metres
(`width`, `height`, `latitude`, `longitude`, `floors`). It does not depend on Fabric,
so the user-side view can read it later with or without the library.
