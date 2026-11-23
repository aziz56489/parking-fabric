import { Component, OnInit, inject } from '@angular/core';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { Router, RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { AuthService } from '../auth.service';
import { CounterReservation, CounterService, NonPayingReport } from './counter.service';

/** SCRUM-88: As a ticket counter, I can report a non paying user. */
@Component({
  selector: 'app-counter',
  standalone: true,
  imports: [FormsModule, RouterLink, DatePipe, CurrencyPipe],
  templateUrl: './counter.component.html',
  styleUrl: './counter.component.css',
})
export class CounterComponent implements OnInit {
  private counter = inject(CounterService);
  private router = inject(Router);
  auth = inject(AuthService);

  tab: 'reservations' | 'reports' = 'reservations';
  query = '';
  reservations: CounterReservation[] = [];
  reports: NonPayingReport[] = [];
  loading = true;
  error = '';
  message = '';

  /** The reservation being reported, and the note typed for it. */
  reporting: CounterReservation | null = null;
  note = '';
  sending = false;

  ngOnInit() {
    this.load();
  }

  load() {
    this.loading = true;
    this.error = '';
    forkJoin([this.counter.getReservations(), this.counter.getReports()]).subscribe({
      next: ([reservations, reports]) => {
        this.reservations = reservations;
        this.reports = reports;
        this.loading = false;
      },
      error: (err: HttpErrorResponse) => {
        this.loading = false;
        if (err.status === 401) {
          this.router.navigate(['/login'], { queryParams: { returnUrl: '/counter' } });
        } else {
          this.error = err.error?.message ?? 'Could not load the reservations';
        }
      },
    });
  }

  get parkedNow() {
    return this.reservations.filter(r => r.active).length;
  }

  get recentExits() {
    return this.reservations.filter(r => !r.active).length;
  }

  get filteredReservations() {
    return this.reservations.filter(r => this.matches(r.userName, r.userEmail, r.spotNumber, r.areaName));
  }

  get filteredReports() {
    return this.reports.filter(r => this.matches(r.userName, r.userEmail, r.spotNumber, r.areaName, r.note));
  }

  private matches(...fields: (string | null)[]) {
    const needle = this.query.trim().toLowerCase();
    return !needle || fields.some(f => f?.toLowerCase().includes(needle));
  }

  canReport(r: CounterReservation) {
    return r.userId !== null && r.userName !== null && !r.reported;
  }

  initials(name: string | null) {
    return (name ?? '?').split(' ').filter(Boolean).slice(0, 2).map(p => p[0].toUpperCase()).join('') || '?';
  }

  duration(minutes: number) {
    const h = Math.floor(minutes / 60);
    return h > 0 ? `${h} h ${minutes % 60} min` : `${minutes} min`;
  }

  openReport(r: CounterReservation) {
    this.reporting = r;
    this.note = '';
    this.message = '';
  }

  cancelReport() {
    this.reporting = null;
  }

  sendReport() {
    const target = this.reporting;
    if (!target) return;
    this.sending = true;
    this.counter.report(target.id, this.note.trim()).subscribe({
      next: report => {
        this.sending = false;
        this.reporting = null;
        this.reports = [report, ...this.reports];
        this.reservations = this.reservations.map(r => (r.id === target.id ? { ...r, reported: true } : r));
        this.message = `${report.userName ?? 'The user'} was reported as non paying.`;
      },
      error: (err: HttpErrorResponse) => {
        this.sending = false;
        this.error = err.error?.message ?? 'Could not send the report';
        this.reporting = null;
      },
    });
  }

  logout() {
    this.auth.logout();
    this.router.navigate(['/login']);
  }
}
