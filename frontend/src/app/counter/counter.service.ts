import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { switchMap } from 'rxjs';

/** SCRUM-88: a reservation as the ticket counter sees it, with its driver. */
export interface CounterReservation {
  id: number;
  userId: number | null;
  userName: string | null;
  userEmail: string | null;
  spotNumber: string;
  areaName: string;
  startedAt: string;
  endedAt: string | null;
  minutes: number;
  amount: number | null;
  currency: string;
  active: boolean;
  reported: boolean;
}

export interface NonPayingReport {
  id: number;
  reservationId: number | null;
  userId: number;
  userName: string | null;
  userEmail: string | null;
  ticketCounterId: number;
  ticketCounterName: string | null;
  spotNumber: string | null;
  areaName: string | null;
  amount: number | null;
  currency: string | null;
  note: string | null;
  reportedAt: string;
  /** The admin's decision: ban the user, or let it go. */
  status: 'PENDING' | 'BANNED' | 'DISMISSED';
  userBlocked: boolean;
  /** Written by the admin when banning; the user sees it in the app. */
  banReason: string | null;
  resolvedByName: string | null;
  resolvedAt: string | null;
}

@Injectable({ providedIn: 'root' })
export class CounterService {
  private http = inject(HttpClient);
  private api = 'http://localhost:8080/api';

  getReservations() {
    return this.http.get<CounterReservation[]>(`${this.api}/counter/reservations`);
  }

  getReports() {
    return this.http.get<NonPayingReport[]>(`${this.api}/counter/non-paying-reports`);
  }

  /** Fetches the CSRF cookie first, which the session interceptor then sends as X-XSRF-TOKEN. */
  report(reservationId: number, note: string) {
    return this.http.get(`${this.api}/auth/csrf`).pipe(
      switchMap(() => this.http.post<NonPayingReport>(`${this.api}/counter/non-paying-reports`, { reservationId, note })),
    );
  }
}
