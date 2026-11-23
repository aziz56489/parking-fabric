import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, switchMap } from 'rxjs';
import { NonPayingReport } from '../counter/counter.service';

export type AppealStatus = 'PENDING' | 'ACCEPTED' | 'REJECTED';

export interface AdminAppeal {
  id: number;
  userId: number;
  userName: string | null;
  userEmail: string | null;
  userBlocked: boolean;
  banReason: string | null;
  message: string;
  status: AppealStatus;
  createdAt: string;
  adminResponse: string | null;
  resolvedByName: string | null;
  resolvedAt: string | null;
}

export type Period = 'DAY' | 'MONTH' | 'YEAR';

/** SCRUM-81 */
export interface EarningsStats {
  period: Period;
  currency: string;
  today: number;
  thisMonth: number;
  thisYear: number;
  buckets: { start: string; label: string; total: number; exits: number }[];
}

/** SCRUM-80 */
export interface EntryStats {
  areaId: number | null;
  days: number;
  today: number;
  averagePerDay: number;
  peak: { date: string; label: string; entries: number } | null;
  daily: { date: string; label: string; entries: number }[];
  areas: { areaId: number; areaName: string; today: number; total: number; averagePerDay: number }[];
}

/** Admin dashboard data: non paying reports, bans, appeals and statistics. */
@Injectable({ providedIn: 'root' })
export class AdminReportsService {
  private http = inject(HttpClient);
  private api = 'http://localhost:8080/api';

  getReports() {
    return this.http.get<NonPayingReport[]>(`${this.api}/admin/non-paying-reports`);
  }

  /** Bans the reported user with a reason they will see in the app; returns every report closed by the ban. */
  ban(reportId: number, reason: string) {
    return this.withCsrf(() =>
      this.http.post<NonPayingReport[]>(`${this.api}/admin/non-paying-reports/${reportId}/ban`, { reason }));
  }

  /** Lets it go: the user is not banned. */
  dismiss(reportId: number) {
    return this.withCsrf(() => this.http.post<NonPayingReport>(`${this.api}/admin/non-paying-reports/${reportId}/dismiss`, {}));
  }

  unban(userId: number) {
    return this.withCsrf(() => this.http.post<void>(`${this.api}/admin/users/${userId}/unban`, {}));
  }

  getAppeals() {
    return this.http.get<AdminAppeal[]>(`${this.api}/admin/appeals`);
  }

  /** Accepting unbans the user. The response is optional and shown to the user. */
  decideAppeal(id: number, decision: 'accept' | 'reject', response: string) {
    return this.withCsrf(() => this.http.post<AdminAppeal>(`${this.api}/admin/appeals/${id}/${decision}`, { response }));
  }

  getEarnings(period: Period) {
    return this.http.get<EarningsStats>(`${this.api}/admin/stats/earnings`, { params: { period } });
  }

  getEntries(areaId: number | null, days = 30) {
    const params: Record<string, string> = { days: String(days) };
    if (areaId !== null) params['areaId'] = String(areaId);
    return this.http.get<EntryStats>(`${this.api}/admin/stats/entries`, { params });
  }

  /** Makes sure the CSRF cookie exists; the session interceptor sends it as X-XSRF-TOKEN. */
  private withCsrf<T>(request: () => Observable<T>) {
    return this.http.get(`${this.api}/auth/csrf`).pipe(switchMap(request));
  }
}
