# Project file guide

This project is a small parking-area layout editor: an Angular frontend talks to a
Spring Boot REST API, which stores areas and spots in MySQL.
The root `README.md` contains startup instructions and a feature overview.

## Backend (`backend/`)

### Build and runtime configuration

- `pom.xml` — Maven dependencies, Java 17 target, Lombok annotation processing, and Spring Boot packaging.
- `src/main/resources/application.properties` — server port, configurable MySQL connection, JPA schema behavior, and SQL logging.
- `src/main/java/com/parking/ParkingLayoutApplication.java` — Spring Boot entry point.

### API and business logic

- `src/main/java/com/parking/controller/ParkingAreaController.java` — REST endpoints for `/api/parking-areas`; allows requests from the local Angular origins (`localhost:4200` and `127.0.0.1:4200`).
- `src/main/java/com/parking/controller/ParkingSpotController.java` — REST endpoint for partially updating a `/api/parking-spots/{id}` resource.
- `src/main/java/com/parking/service/ParkingService.java` — creates and retrieves areas, replaces layouts, updates spots, and protects reserved or occupied spots from removal.
- `src/main/java/com/parking/dto/ParkingRequests.java` — request records for area creation, layout replacement, and partial spot updates.

### Persistence model

- `src/main/java/com/parking/entity/ParkingArea.java` — parking area entity, its spots, layout JSON, capacity, and occupancy rate.
- `src/main/java/com/parking/entity/ParkingSpot.java` — parking spot entity and status/type updates.
- `src/main/java/com/parking/entity/SpotType.java` — supported spot types.
- `src/main/java/com/parking/entity/SpotStatus.java` — supported spot statuses.
- `src/main/java/com/parking/repository/ParkingAreaRepository.java` — database operations for parking areas, including duplicate-name lookup.
- `src/main/java/com/parking/repository/ParkingSpotRepository.java` — database operations for parking spots.

## Frontend (`frontend/`)

### Angular setup

- `package.json` — frontend dependencies and start/build scripts.
- `package-lock.json` — exact resolved npm dependency versions for repeatable installs.
- `angular.json` — Angular build and development-server configuration.
- `tsconfig.json` — shared TypeScript and Angular compiler options.
- `tsconfig.app.json` — TypeScript settings and entry point for the application build.
- `src/index.html` — browser document hosting the Angular root component.
- `src/main.ts` — bootstraps the Angular application.
- `src/styles.css` — shared page, form, table, and message styles.

### Application shell and data access

- `src/app/app.component.ts` — root component containing the router outlet.
- `src/app/app.config.ts` — registers Angular routing, HTTP, and change detection.
- `src/app/app.routes.ts` — maps the root route to the area list and area URLs to the layout editor.
- `src/app/models.ts` — shared TypeScript types for areas, spots, layouts, and editor items.
- `src/app/parking.service.ts` — HTTP client for the backend API.

### Screens and layout editor

- `src/app/parking-list/parking-list.component.ts` — loads and creates parking areas.
- `src/app/parking-list/parking-list.component.html` — area creation form and area list.
- `src/app/layout-editor/layout-editor.component.ts` — editor state and behavior: floors, item selection, drag-and-drop, undo/redo, and saving.
- `src/app/layout-editor/layout-editor.component.html` — editor toolbar, item palette, plan canvas, and selected-item controls.
- `src/app/layout-editor/layout-editor.component.css` — editor panels, toolbar, palette, and canvas layout.
- `src/app/layout-editor/plan-items.ts` — creates and reads Fabric.js canvas objects, snaps positions to the grid, and detects overlaps.

## Security note

The API is unauthenticated. CORS is enabled for the two local development origins so the
Angular app can call the backend. Do not expose it publicly without adding appropriate
access control.

The backend connects to MySQL at `localhost:3306/parking_db` by default. Configure `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` for your MySQL instance. Hibernate updates the schema without dropping existing data.
