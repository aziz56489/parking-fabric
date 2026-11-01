import { Component, OnInit, inject } from '@angular/core';
import { PercentPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ParkingService } from '../parking.service';
import { ParkingArea } from '../models';

@Component({
  selector: 'app-parking-list',
  standalone: true,
  imports: [FormsModule, RouterLink, PercentPipe],
  templateUrl: './parking-list.component.html',
})
export class ParkingListComponent implements OnInit {
  private parkingService = inject(ParkingService);
  private router = inject(Router);

  areas: ParkingArea[] = [];
  newName = '';
  error = '';

  ngOnInit() {
    this.parkingService.getParkingAreas().subscribe(areas => (this.areas = areas));
  }

  create() {
    this.parkingService.createParkingArea(this.newName).subscribe({
      next: area => this.router.navigate(['/parking-areas', area.id]),
      error: (err: HttpErrorResponse) => (this.error = err.error?.message ?? 'Could not create the parking area'),
    });
  }
}
