export type SpotType = 'REGULAR' | 'EV' | 'HANDICAPPED' | 'PERSONNEL';
export type SpotStatus = 'AVAILABLE' | 'RESERVED' | 'OCCUPIED' | 'NOT_AVAILABLE';

export interface ParkingSpot {
  id: number;
  number: string;
  type: SpotType;
  status: SpotStatus;
}

export type SpotUpdateRequest =
  | { type: SpotType; status?: SpotStatus }
  | { type?: SpotType; status: SpotStatus };

export interface ParkingArea {
  id: number;
  name: string;
  layout: string | null;
  capacity: number;
  occupancyRate: number;
  spots: ParkingSpot[];
}

export type ItemKind = 'SPOT' | 'ROAD' | 'ENTRANCE' | 'EXIT' | 'PILLAR' | 'WALL' | 'GREEN_SPACE';

export interface Item {
  kind: ItemKind;
  type?: SpotType;
  number?: string;
  x: number;
  y: number;
  width: number;
  height: number;
  angle: number;
}

export interface Layout {
  width: number;
  height: number;
  latitude: number | null;
  longitude: number | null;
  floors: Item[][];
}

export interface PaletteEntry {
  label: string;
  kind: ItemKind;
  type?: SpotType;
  width: number;
  height: number;
}

export interface LayoutRequest {
  layout: string;
  spots: { number: string; type: SpotType }[];
}
