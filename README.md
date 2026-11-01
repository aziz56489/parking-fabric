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
cd backend
mvn spring-boot:run
```

H2 console: http://localhost:8080/h2-console (JDBC URL `jdbc:h2:mem:parking_db`, user `sa`, no password)

## Run the frontend

```
cd frontend
npm install
npm start
```

Open http://localhost:4200, create a parking area, then click "Edit layout".

## REST API

- `GET /api/parking-areas` — list parking areas.
- `POST /api/parking-areas` — create an area; returns `201 Created` with a `Location` header.
- `GET /api/parking-areas/{id}` — retrieve one area and its spots.
- `PUT /api/parking-areas/{id}/layout` — replace an area's layout and spot collection.
- `GET /api/parking-spots/{id}` — retrieve one spot.
- `PATCH /api/parking-spots/{id}` — update a spot's `type`, `status`, or both. For example, send `{"status":"NOT_AVAILABLE"}` to make a spot unavailable.

All endpoints return JSON. The API does not include authentication.

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
