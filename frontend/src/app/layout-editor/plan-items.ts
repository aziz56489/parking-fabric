import { FabricText, Group, Rect } from 'fabric';
import { Item, ItemKind, SpotStatus } from '../models';

export const PIXELS_PER_METER = 20;

const COLORS: Record<string, string> = {
  REGULAR: '#2563eb',
  EV: '#16a34a',
  HANDICAPPED: '#9333ea',
  PERSONNEL: '#ea580c',
  ROAD: '#52525b',
  ENTRANCE: '#ca8a04',
  EXIT: '#dc2626',
  PILLAR: '#78350f',
  WALL: '#78350f',
  GREEN_SPACE: '#65a30d',
};

const LAYERS: Record<ItemKind, number> = {
  GREEN_SPACE: 0,
  ROAD: 1,
  ENTRANCE: 2,
  EXIT: 2,
  SPOT: 3,
  PILLAR: 4,
  WALL: 4,
};

export function snapToHalfMeter(meters: number): number {
  return Math.round(meters * 2) / 2;
}

export function byLayer(a: Item, b: Item): number {
  return LAYERS[a.kind] - LAYERS[b.kind];
}

export function createPlanObject(item: Item, status: SpotStatus | null, overlapping: boolean): Group {
  const width = item.width * PIXELS_PER_METER;
  const height = item.height * PIXELS_PER_METER;

  const rect = new Rect({
    width,
    height,
    fill: fillColor(item, status),
    stroke: strokeColor(status, overlapping),
    strokeWidth: item.kind === 'SPOT' ? 2 : 0,
    strokeUniform: true,
    opacity: item.kind === 'SPOT' && status === null ? 0.75 : 1,
  });

  const label = new FabricText(labelOf(item), {
    left: width / 2,
    top: height / 2,
    originX: 'center',
    originY: 'center',
    fontSize: 14,
    fontWeight: 'bold',
    fontFamily: 'sans-serif',
    fill: 'white',
  });

  const group = new Group([rect, label]);
  group.set({
    left: item.x * PIXELS_PER_METER,
    top: item.y * PIXELS_PER_METER,
    angle: item.angle,
    snapAngle: 15,
    lockScalingFlip: true,
    transparentCorners: false,
    cornerColor: '#2563eb',
    borderColor: '#2563eb',
  });
  group.setCoords();
  return group;
}

export function readPlanObject(group: Group, item: Item): Item {
  const rect = group.getObjects()[0];
  return {
    ...item,
    x: snapToHalfMeter(group.left / PIXELS_PER_METER),
    y: snapToHalfMeter(group.top / PIXELS_PER_METER),
    width: Math.max(0.5, snapToHalfMeter((rect.width * group.scaleX) / PIXELS_PER_METER)),
    height: Math.max(0.5, snapToHalfMeter((rect.height * group.scaleY) / PIXELS_PER_METER)),
    angle: ((Math.round(group.angle) % 360) + 360) % 360,
  };
}

export function findOverlaps(items: Item[]): Set<Item> {
  const overlapping = new Set<Item>();
  const boxes = items.map(boundingBox);
  for (let i = 0; i < items.length; i++) {
    for (let j = i + 1; j < items.length; j++) {
      const involvesSpot = items[i].kind === 'SPOT' || items[j].kind === 'SPOT';
      if (involvesSpot && boxesOverlap(boxes[i], boxes[j])) {
        overlapping.add(items[i]);
        overlapping.add(items[j]);
      }
    }
  }
  return overlapping;
}

function fillColor(item: Item, status: SpotStatus | null): string {
  if (status === 'NOT_AVAILABLE') {
    return '#a1a1aa';
  }
  return COLORS[item.type ?? item.kind];
}

function strokeColor(status: SpotStatus | null, overlapping: boolean): string {
  if (overlapping) return '#dc2626';
  if (status === 'RESERVED') return '#facc15';
  if (status === 'OCCUPIED') return '#18181b';
  return 'white';
}

function labelOf(item: Item): string {
  switch (item.kind) {
    case 'SPOT':
      return item.number ?? '';
    case 'ENTRANCE':
      return 'IN';
    case 'EXIT':
      return 'OUT';
    default:
      return '';
  }
}

interface Box {
  left: number;
  top: number;
  right: number;
  bottom: number;
}

function boundingBox(item: Item): Box {
  const radians = (item.angle * Math.PI) / 180;
  const cos = Math.cos(radians);
  const sin = Math.sin(radians);
  const corners = [
    [0, 0],
    [item.width, 0],
    [0, item.height],
    [item.width, item.height],
  ].map(([w, h]) => [item.x + w * cos - h * sin, item.y + w * sin + h * cos]);
  const xs = corners.map(([x]) => x);
  const ys = corners.map(([, y]) => y);
  return { left: Math.min(...xs), top: Math.min(...ys), right: Math.max(...xs), bottom: Math.max(...ys) };
}

function boxesOverlap(a: Box, b: Box): boolean {
  const margin = 0.01;
  return a.left < b.right - margin && b.left < a.right - margin && a.top < b.bottom - margin && b.top < a.bottom - margin;
}
