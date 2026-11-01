import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { LayoutRequest, ParkingArea, ParkingSpot, SpotUpdateRequest } from './models';

@Injectable({ providedIn: 'root' })
export class ParkingService {
  private http = inject(HttpClient);
  private api = 'http://localhost:8080/api';

  getParkingAreas() {
    return this.http.get<ParkingArea[]>(`${this.api}/parking-areas`);
  }

  getParkingArea(id: number) {
    return this.http.get<ParkingArea>(`${this.api}/parking-areas/${id}`);
  }

  createParkingArea(name: string) {
    return this.http.post<ParkingArea>(`${this.api}/parking-areas`, { name });
  }

  updateLayout(areaId: number, request: LayoutRequest) {
    return this.http.put<ParkingArea>(`${this.api}/parking-areas/${areaId}/layout`, request);
  }

  updateParkingSpot(spotId: number, changes: SpotUpdateRequest) {
    return this.http.patch<ParkingSpot>(`${this.api}/parking-spots/${spotId}`, changes);
  }
}
