# Parking area creation and layout editor

Provides a JSON REST API for parking areas and spots, backed by an Angular layout editor.
The domain model includes `ParkingArea`, `ParkingSpot`, `SpotType`, and `SpotStatus`.

The 2D editor uses **Fabric.js** (https://fabricjs.com), an open-source canvas library.
Fabric handles selecting, moving, resizing and rotating items with handles.
Our code only creates the shapes, snaps them to the 0.5 m grid and reads their
position back when the admin saves.

## Run the backend

Requires JDK 17 and Maven. From the repository root, run:

```powershell
$env:ADMIN_PASSWORD = "choose-a-strong-admin-password"
$env:DB_PASSWORD = "your-supabase-database-password"
cd backend
mvn spring-boot:run
```

On first startup, the backend creates the initial administrator using `ADMIN_EMAIL` (default `admin@parking.com`) and `ADMIN_PASSWORD`. If `ADMIN_PASSWORD` is unset, it skips admin creation; public registration always creates a regular user.

The backend connects to the Supabase PostgreSQL pooler. The default URL and username point at this project's Supabase pooler; set `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` in the environment to use another connection or password. The schema is updated on startup and data is retained when the backend stops. Keep database credentials in environment variables, not in this file.

## Run the frontend

```
cd frontend
npm install
npm start
```

Open http://localhost:4200, create a parking area, then click "Edit layout".

## Run the Android app

The Android emulator uses `http://10.0.2.2:8080/api/` to reach the backend on the host machine. For a physical device, set `api.url` in `android/local.properties` to the host computer's LAN address, for example `http://192.168.1.20:8080/api/`, and keep the phone and computer on the same network.

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
