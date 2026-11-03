import { AfterViewInit, Component, ElementRef, HostListener, OnDestroy, ViewChild, inject } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { ActiveSelection, Canvas, FabricObject, Group } from 'fabric';
import { ParkingService } from '../parking.service';
import { Item, ItemKind, Layout, PaletteEntry, ParkingArea, ParkingSpot, SpotStatus, SpotType } from '../models';
import { PIXELS_PER_METER, byLayer, createPlanObject, findOverlaps, readPlanObject, snapToHalfMeter } from './plan-items';

@Component({
  selector: 'app-layout-editor',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './layout-editor.component.html',
  styleUrl: './layout-editor.component.css',
})
export class LayoutEditorComponent implements AfterViewInit, OnDestroy {
  private route = inject(ActivatedRoute);
  private parkingService = inject(ParkingService);

  @ViewChild('canvas') canvasElement!: ElementRef<HTMLCanvasElement>;

  readonly spotTypes: SpotType[] = ['REGULAR', 'EV', 'HANDICAPPED', 'PERSONNEL'];
  readonly palette: PaletteEntry[] = [
    { label: 'Regular spot', kind: 'SPOT', type: 'REGULAR', width: 2.5, height: 5 },
    { label: 'EV spot', kind: 'SPOT', type: 'EV', width: 2.5, height: 5 },
    { label: 'Handicapped spot', kind: 'SPOT', type: 'HANDICAPPED', width: 3.5, height: 5 },
    { label: 'Personnel spot', kind: 'SPOT', type: 'PERSONNEL', width: 2.5, height: 5 },
    { label: 'Road', kind: 'ROAD', width: 10, height: 6 },
    { label: 'Entrance', kind: 'ENTRANCE', width: 4, height: 2 },
    { label: 'Exit', kind: 'EXIT', width: 4, height: 2 },
    { label: 'Pillar', kind: 'PILLAR', width: 1, height: 1 },
    { label: 'Wall', kind: 'WALL', width: 10, height: 0.5 },
    { label: 'Green space', kind: 'GREEN_SPACE', width: 4, height: 4 },
  ];
  readonly kindNames: Record<ItemKind, string> = {
    SPOT: 'Spot',
    ROAD: 'Road',
    ENTRANCE: 'Entrance',
    EXIT: 'Exit',
    PILLAR: 'Pillar',
    WALL: 'Wall',
    GREEN_SPACE: 'Green space',
  };

  area: ParkingArea | null = null;
  planWidth = 80;
  planHeight = 50;
  floors: Item[][] = [[]];
  floor = 0;
  selected: Item | null = null;
  zoom = 1;
  rowCount = 5;
  message = '';

  latitude: number | null = null;
  longitude: number | null = null;
  showSatellite = false;
  uploadedImage: string | null = null;
  backgroundOpacity = 0.6;
  backgroundMoveEnabled = false;
  backgroundOffsetX = 0;
  backgroundOffsetY = 0;
  backgroundRotation = 0;
  backgroundScale = 1;
  private backgroundNativeWidth = 0;
  private backgroundNativeHeight = 0;

  private canvas!: Canvas;
  private objects = new Map<Group, Item>();
  private undoStack: Item[][] = [];
  private redoStack: Item[][] = [];
  private draggedEntry: PaletteEntry | null = null;
  private backgroundInteraction: {
    mode: 'move' | 'resize' | 'rotate';
    pointerId: number;
    clientX: number;
    clientY: number;
    offsetX: number;
    offsetY: number;
    scale: number;
    rotation: number;
    centerX: number;
    centerY: number;
    startDistance: number;
    startAngle: number;
  } | null = null;

  ngAfterViewInit() {
    // Fabric's marquee selection is disabled by default when selection is false.
    // Keep stacking order stable so overlapping components can still be picked.
    this.canvas = new Canvas(this.canvasElement.nativeElement, {
      selection: true,
      selectionKey: ['shiftKey', 'ctrlKey', 'metaKey'],
      preserveObjectStacking: true,
    });
    this.canvas.on('object:moving', ({ target }) => {
      // ActiveSelection's left/top are its bounding box, not a component's
      // canvas position. Snapping that wrapper can pull the whole selection to
      // (0, 0), so snap multi-selections after Fabric restores their objects.
      if (!(target instanceof ActiveSelection)) {
        this.snapToGrid(target);
      }
    });
    this.canvas.on('object:modified', () => queueMicrotask(() => this.saveCanvasChanges()));
    this.canvas.on('selection:created', () => this.updateSelected());
    this.canvas.on('selection:updated', () => this.updateSelected());
    this.canvas.on('selection:cleared', () => this.updateSelected());

    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.parkingService.getParkingArea(id).subscribe(area => this.load(area));
  }

  ngOnDestroy() {
    this.canvas.dispose();
  }

  get items(): Item[] {
    return this.floors[this.floor];
  }

  get pixelWidth() {
    return this.planWidth * PIXELS_PER_METER * this.zoom;
  }

  get pixelHeight() {
    return this.planHeight * PIXELS_PER_METER * this.zoom;
  }

  get gridBackgroundSize() {
    const halfMeter = (PIXELS_PER_METER / 2) * this.zoom;
    const fiveMeters = halfMeter * 10;
    return `${halfMeter}px ${halfMeter}px, ${halfMeter}px ${halfMeter}px, ${fiveMeters}px ${fiveMeters}px, ${fiveMeters}px ${fiveMeters}px`;
  }

  get canUndo() {
    return this.undoStack.length > 0;
  }

  get canRedo() {
    return this.redoStack.length > 0;
  }

  get backgroundImage(): string | null {
    return this.uploadedImage ?? this.satelliteUrl;
  }

  get backgroundTransform(): string {
    const pixelsPerMeter = PIXELS_PER_METER * this.zoom;
    const x = this.backgroundOffsetX * pixelsPerMeter;
    const y = this.backgroundOffsetY * pixelsPerMeter;
    return `translate(${x}px, ${y}px) rotate(${this.backgroundRotation}deg) scale(${this.backgroundScale})`;
  }

  get backgroundPixelWidth(): number {
    return this.backgroundNativeWidth * this.zoom;
  }

  get backgroundPixelHeight(): number {
    return this.backgroundNativeHeight * this.zoom;
  }

  get backgroundRotationRounded(): number {
    return Math.round(this.backgroundRotation);
  }

  get satelliteUrl(): string | null {
    if (!this.showSatellite || this.latitude === null || this.longitude === null) {
      return null;
    }
    const metersPerDegree = 111_320;
    const halfHeight = this.planHeight / 2 / metersPerDegree;
    const halfWidth = this.planWidth / 2 / (metersPerDegree * Math.cos((this.latitude * Math.PI) / 180));
    const box = [
      this.longitude - halfWidth,
      this.latitude - halfHeight,
      this.longitude + halfWidth,
      this.latitude + halfHeight,
    ].join(',');
    const size = `${this.planWidth * PIXELS_PER_METER},${this.planHeight * PIXELS_PER_METER}`;
    return 'https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/export'
      + `?bbox=${box}&bboxSR=4326&imageSR=3857&size=${size}&format=jpg&f=image`;
  }

  floorLetter(floor: number) {
    return String.fromCharCode(65 + floor);
  }

  countSpots(type?: SpotType) {
    return this.floors.flat().filter(item => item.kind === 'SPOT' && (!type || item.type === type)).length;
  }

  statusOf(item: Item): SpotStatus | null {
    if (item.kind !== 'SPOT') {
      return null;
    }
    return this.savedSpot(item)?.status ?? null;
  }

  // ---------- palette ----------

  startPaletteDrag(entry: PaletteEntry, event: DragEvent) {
    this.draggedEntry = entry;
    if (event.dataTransfer) {
      event.dataTransfer.effectAllowed = 'copy';
      event.dataTransfer.setData('text/plain', entry.label);
    }
  }

  endPaletteDrag() {
    this.draggedEntry = null;
  }

  allowDrop(event: DragEvent) {
    event.preventDefault();
    if (event.dataTransfer) {
      event.dataTransfer.dropEffect = 'copy';
    }
  }

  dropOnPlan(event: DragEvent) {
    event.preventDefault();
    const entry = this.draggedEntry;
    if (!entry) {
      return;
    }
    const plan = (event.currentTarget as HTMLElement).getBoundingClientRect();
    const scale = PIXELS_PER_METER * this.zoom;
    const item: Item = {
      kind: entry.kind,
      type: entry.type,
      number: entry.type ? this.nextSpotNumbers(1)[0] : undefined,
      x: snapToHalfMeter((event.clientX - plan.left) / scale - entry.width / 2),
      y: snapToHalfMeter((event.clientY - plan.top) / scale - entry.height / 2),
      width: entry.width,
      height: entry.height,
      angle: 0,
    };
    this.commit([...this.items, item], item);
  }

  // ---------- selected item ----------

  changeSelected(changes: Partial<Item>) {
    const selected = this.selected;
    if (!selected) {
      return;
    }
    const updated = { ...selected, ...changes };
    this.commit(this.items.map(item => (item === selected ? updated : item)), updated);
  }

  setMeters(field: 'x' | 'y' | 'width' | 'height', meters: number) {
    const minimum = field === 'x' || field === 'y' ? 0 : 0.5;
    this.changeSelected({ [field]: Math.max(minimum, snapToHalfMeter(meters)) });
  }

  rotate() {
    if (this.selected) {
      this.changeSelected({ angle: (this.selected.angle + 90) % 360 });
    }
  }

  addCopies(count: number) {
    const source = this.selected;
    if (!source || count < 1) {
      return;
    }
    const radians = (source.angle * Math.PI) / 180;
    const stepX = source.width * Math.cos(radians);
    const stepY = source.width * Math.sin(radians);
    const numbers = source.kind === 'SPOT' ? this.nextSpotNumbers(count) : [];
    const copies = Array.from({ length: count }, (_, i) => ({
      ...source,
      number: numbers[i],
      x: snapToHalfMeter(source.x + stepX * (i + 1)),
      y: snapToHalfMeter(source.y + stepY * (i + 1)),
    }));
    this.commit([...this.items, ...copies], copies[copies.length - 1]);
  }

  remove() {
    const selected = this.selected;
    if (!selected) {
      return;
    }
    if (this.isInUse(selected)) {
      this.message = `Spot ${selected.number} is reserved or occupied, it cannot be deleted`;
      return;
    }
    this.commit(this.items.filter(item => item !== selected), null);
  }

  changeType(type: SpotType) {
    const saved = this.selected && this.savedSpot(this.selected);
    if (!saved) {
      this.changeSelected({ type });
      return;
    }
    this.parkingService.updateParkingSpot(saved.id, { type }).subscribe({
      next: spot => {
        this.replaceSpot(spot);
        this.changeSelected({ type });
      },
      error: err => this.showError(err),
    });
  }

  makeUnavailable() {
    const saved = this.selected && this.savedSpot(this.selected);
    if (!saved) {
      return;
    }
    this.parkingService.updateParkingSpot(saved.id, { status: 'NOT_AVAILABLE' }).subscribe({
      next: spot => {
        this.replaceSpot(spot);
        this.draw(this.items, this.selected);
      },
      error: err => this.showError(err),
    });
  }
  makeAvailable() {
    const saved = this.selected && this.savedSpot(this.selected);
    if (!saved) {
      return;
    }
    this.parkingService.updateParkingSpot(saved.id, { status: 'AVAILABLE' }).subscribe({
      next: spot => {
        this.replaceSpot(spot);
        this.draw(this.items, this.selected);
      },
      error: err => this.showError(err),
    });
  }

  // ---------- floors, plan, zoom, background ----------

  switchFloor(floor: number) {
    this.floor = floor;
    this.undoStack = [];
    this.redoStack = [];
    this.draw(this.items, null);
  }

  addFloor() {
    this.floors.push([]);
    this.switchFloor(this.floors.length - 1);
  }

  removeLastFloor() {
    const last = this.floors[this.floors.length - 1];
    if (last.some(item => this.isInUse(item))) {
      this.message = 'The last level has reserved or occupied spots';
      return;
    }
    this.floors.pop();
    this.switchFloor(Math.min(this.floor, this.floors.length - 1));
  }

  setPlanSize(side: 'width' | 'height', meters: number) {
    const value = Math.max(10, snapToHalfMeter(meters));
    if (side === 'width') {
      this.planWidth = value;
    } else {
      this.planHeight = value;
    }
    this.resizeCanvas();
  }

  zoomBy(step: number) {
    this.zoom = Math.min(3, Math.max(0.25, this.zoom + step));
    this.resizeCanvas();
  }

  setCoordinate(axis: 'latitude' | 'longitude', value: string) {
    const number = Number(value.replace(',', '.'));
    this[axis] = value.trim() === '' || isNaN(number) ? null : number;
  }

  toggleSatellite() {
    if (this.latitude === null || this.longitude === null) {
      this.message = 'Enter the latitude and longitude of the parking center first';
      return;
    }
    this.uploadedImage = null;
    this.showSatellite = !this.showSatellite;
    this.resetBackgroundTransform();
  }

  uploadImage(event: Event) {
    const file = (event.target as HTMLInputElement).files?.[0];
    if (!file) {
      return;
    }
    const reader = new FileReader();
    reader.onload = () => {
      this.uploadedImage = reader.result as string;
      this.showSatellite = false;
      this.resetBackgroundTransform();
    };
    reader.readAsDataURL(file);
  }

  removeBackground() {
    this.uploadedImage = null;
    this.showSatellite = false;
    this.backgroundMoveEnabled = false;
    this.resetBackgroundTransform();
    this.backgroundNativeWidth = 0;
    this.backgroundNativeHeight = 0;
  }

  toggleBackgroundMove() {
    this.backgroundMoveEnabled = !this.backgroundMoveEnabled;
    this.backgroundInteraction = null;
  }

  rotateBackground() {
    this.backgroundRotation = (this.backgroundRotation + 90) % 360;
  }

  setBackgroundScale(percent: number) {
    this.backgroundScale = Math.min(3, Math.max(0.25, percent / 100));
  }

  setBackgroundDimensions(event: Event) {
    const image = event.currentTarget as HTMLImageElement;
    this.backgroundNativeWidth = image.naturalWidth;
    this.backgroundNativeHeight = image.naturalHeight;
  }

  startBackgroundInteraction(event: PointerEvent, mode: 'move' | 'resize' | 'rotate') {
    if (!this.backgroundMoveEnabled) {
      return;
    }
    event.preventDefault();
    event.stopPropagation();
    (event.currentTarget as HTMLElement).setPointerCapture(event.pointerId);
    const plan = this.canvasElement.nativeElement.closest('.plan') as HTMLElement | null;
    if (!plan) {
      return;
    }
    const planBounds = plan.getBoundingClientRect();
    const pixelsPerMeter = PIXELS_PER_METER * this.zoom;
    const centerX = planBounds.left + this.backgroundOffsetX * pixelsPerMeter + this.backgroundPixelWidth / 2;
    const centerY = planBounds.top + this.backgroundOffsetY * pixelsPerMeter + this.backgroundPixelHeight / 2;
    const deltaX = event.clientX - centerX;
    const deltaY = event.clientY - centerY;
    this.backgroundInteraction = {
      mode,
      pointerId: event.pointerId,
      clientX: event.clientX,
      clientY: event.clientY,
      offsetX: this.backgroundOffsetX,
      offsetY: this.backgroundOffsetY,
      scale: this.backgroundScale,
      rotation: this.backgroundRotation,
      centerX,
      centerY,
      startDistance: Math.max(1, Math.hypot(deltaX, deltaY)),
      startAngle: Math.atan2(deltaY, deltaX),
    };
  }

  moveBackgroundInteraction(event: PointerEvent) {
    const interaction = this.backgroundInteraction;
    if (!interaction || interaction.pointerId !== event.pointerId) {
      return;
    }
    event.preventDefault();
    event.stopPropagation();
    if (interaction.mode === 'move') {
      const pixelsPerMeter = PIXELS_PER_METER * this.zoom;
      this.backgroundOffsetX = interaction.offsetX + (event.clientX - interaction.clientX) / pixelsPerMeter;
      this.backgroundOffsetY = interaction.offsetY + (event.clientY - interaction.clientY) / pixelsPerMeter;
    } else if (interaction.mode === 'resize') {
      const distance = Math.hypot(event.clientX - interaction.centerX, event.clientY - interaction.centerY);
      this.backgroundScale = Math.min(3, Math.max(0.25, interaction.scale * distance / interaction.startDistance));
    } else {
      const angle = Math.atan2(event.clientY - interaction.centerY, event.clientX - interaction.centerX);
      const degrees = (angle - interaction.startAngle) * 180 / Math.PI;
      this.backgroundRotation = ((interaction.rotation + degrees) % 360 + 360) % 360;
    }
  }

  finishBackgroundInteraction(event: PointerEvent) {
    if (this.backgroundInteraction?.pointerId === event.pointerId) {
      this.backgroundInteraction = null;
    }
  }

  private resetBackgroundTransform() {
    this.backgroundOffsetX = 0;
    this.backgroundOffsetY = 0;
    this.backgroundRotation = 0;
    this.backgroundScale = 1;
  }

  // ---------- history and saving ----------

  undo() {
    const previous = this.undoStack.pop();
    if (previous) {
      this.redoStack.push(this.items);
      this.draw(previous, null);
    }
  }

  redo() {
    const next = this.redoStack.pop();
    if (next) {
      this.undoStack.push(this.items);
      this.draw(next, null);
    }
  }

  save() {
    if (!this.area) {
      return;
    }
    const layout: Layout = {
      width: this.planWidth,
      height: this.planHeight,
      latitude: this.latitude,
      longitude: this.longitude,
      floors: this.floors,
    };
    const spots = this.floors
      .flat()
      .filter(item => item.kind === 'SPOT')
      .map(item => ({ number: item.number ?? '', type: item.type ?? 'REGULAR' }));

    this.parkingService.updateLayout(this.area.id, { layout: JSON.stringify(layout), spots }).subscribe({
      next: area => {
        this.area = area;
        this.draw(this.items, this.selected);
        this.message = `Layout saved, capacity: ${area.capacity} spots`;
      },
      error: err => this.showError(err),
    });
  }

  @HostListener('window:keydown', ['$event'])
  onKeyDown(event: KeyboardEvent) {
    if (event.target instanceof HTMLInputElement || event.target instanceof HTMLSelectElement) {
      return;
    }
    const ctrl = event.ctrlKey || event.metaKey;
    const key = event.key.toLowerCase();
    if (key === 'delete') this.remove();
    else if (ctrl && key === 'z') this.undo();
    else if (ctrl && key === 'y') this.redo();
    else if (ctrl && key === 'd') this.addCopies(1);
    else if (ctrl && key === 's') this.save();
    else return;
    event.preventDefault();
  }

  // ---------- canvas ----------

  private load(area: ParkingArea) {
    const layout: Layout | null = area.layout ? JSON.parse(area.layout) : null;
    this.area = area;
    this.planWidth = layout?.width ?? 80;
    this.planHeight = layout?.height ?? 50;
    this.latitude = layout?.latitude ?? null;
    this.longitude = layout?.longitude ?? null;
    this.floors = layout?.floors ?? [[]];
    for (const item of this.floors.flat()) {
      const saved = this.savedSpot(item);
      if (saved) {
        item.type = saved.type;
      }
    }
    this.resizeCanvas();
    this.switchFloor(0);
  }

  private commit(items: Item[], selected: Item | null) {
    this.undoStack.push(this.items);
    this.redoStack = [];
    this.message = '';
    this.draw(items, selected);
  }

  private draw(items: Item[], selected: Item | null) {
    this.floors[this.floor] = items;
    this.canvas.clear();
    this.objects.clear();
    const overlapping = findOverlaps(items);
    for (const item of [...items].sort(byLayer)) {
      const object = createPlanObject(item, this.statusOf(item), overlapping.has(item));
      this.objects.set(object, item);
      this.canvas.add(object);
      if (item === selected) {
        this.canvas.setActiveObject(object);
      }
    }
    this.canvas.requestRenderAll();
    this.selected = selected;
  }

  private saveCanvasChanges() {
    const activeObjects = new Set(this.canvas.getActiveObjects());
    if (activeObjects.size > 1) {
      // Unwrap before reading coordinates: while grouped, child left/top values
      // are relative to the ActiveSelection rather than to the canvas.
      this.canvas.discardActiveObject();
    }
    const items: Item[] = [];
    const selectedItems: Item[] = [];
    for (const [object, item] of this.objects) {
      const updated = readPlanObject(object, item);
      items.push(updated);
      if (activeObjects.has(object)) {
        selectedItems.push(updated);
      }
    }
    this.commit(items, selectedItems[0] ?? null);

    // Redrawing after a move/resize replaces Fabric objects. Rebuild the active
    // selection from the new objects so the whole group remains selected.
    if (selectedItems.length > 1) {
      const selectedObjects = [...this.objects]
        .filter(([, item]) => selectedItems.includes(item))
        .map(([object]) => object);
      this.canvas.setActiveObject(new ActiveSelection(selectedObjects, { canvas: this.canvas }));
      this.canvas.requestRenderAll();
      this.updateSelected();
    }
  }

  private updateSelected() {
    const activeObjects = this.canvas.getActiveObjects();
    // The inspector edits a single component, so use the first selected item
    // as its representative while Fabric retains the full multi-selection.
    const active = activeObjects[0];
    this.selected = active instanceof Group || active instanceof ActiveSelection
      ? this.objects.get(active as Group) ?? null
      : null;
  }

  private snapToGrid(object: FabricObject) {
    const step = PIXELS_PER_METER / 2;
    const maxLeft = this.planWidth * PIXELS_PER_METER;
    const maxTop = this.planHeight * PIXELS_PER_METER;
    object.set({
      left: Math.min(Math.max(0, Math.round(object.left / step) * step), maxLeft),
      top: Math.min(Math.max(0, Math.round(object.top / step) * step), maxTop),
    });
  }

  private resizeCanvas() {
    this.canvas.setDimensions({ width: this.pixelWidth, height: this.pixelHeight });
    this.canvas.setZoom(this.zoom);
  }

  // ---------- spots ----------

  savedSpot(item: Item): ParkingSpot | undefined {
    return this.area?.spots.find(spot => spot.number === item.number);
  }

  private isInUse(item: Item): boolean {
    const status = this.statusOf(item);
    return status === 'RESERVED' || status === 'OCCUPIED';
  }

  private nextSpotNumbers(count: number): string[] {
    const letter = this.floorLetter(this.floor);
    const used = this.floors
      .flat()
      .map(item => item.number?.match(new RegExp(`^${letter}-(\\d+)$`)))
      .map(match => (match ? Number(match[1]) : 0));
    const start = Math.max(0, ...used) + 1;
    return Array.from({ length: count }, (_, i) => `${letter}-${String(start + i).padStart(2, '0')}`);
  }

  private replaceSpot(updated: ParkingSpot) {
    if (this.area) {
      this.area.spots = this.area.spots.map(spot => (spot.id === updated.id ? updated : spot));
    }
  }

  private showError(err: HttpErrorResponse) {
    this.message = err.error?.message ?? 'Something went wrong';
  }
}
