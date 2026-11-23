import { Component, OnInit, inject } from '@angular/core';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { NonPayingReport } from '../counter/counter.service';
import { AdminReportsService } from './admin-reports.service';

type Filter = 'PENDING' | 'BANNED' | 'DISMISSED' | 'ALL';

/** Admin dashboard panel: non paying reports, with "Ban user" or "Let it go" for each. */
@Component({
  selector: 'app-admin-reports',
  standalone: true,
  imports: [DatePipe, CurrencyPipe, FormsModule],
  templateUrl: './admin-reports.component.html',
  styleUrl: './admin-reports.component.css',
})
export class AdminReportsComponent implements OnInit {
  private service = inject(AdminReportsService);

  reports: NonPayingReport[] = [];
  filter: Filter = 'PENDING';
  loading = true;
  error = '';
  message = '';
  /** Id of the report whose action is running, to disable its buttons. */
  busy: number | null = null;
  /** The report the admin is about to ban for, waiting for confirmation. */
  confirmBan: NonPayingReport | null = null;
  /** Required: the banned user reads it in the app. */
  banReason = '';

  readonly filters: { value: Filter; label: string }[] = [
    { value: 'PENDING', label: 'To review' },
    { value: 'BANNED', label: 'Banned' },
    { value: 'DISMISSED', label: 'Let go' },
    { value: 'ALL', label: 'All' },
  ];

  ngOnInit() {
    this.load();
  }

  load() {
    this.loading = true;
    this.service.getReports().subscribe({
      next: reports => {
        this.reports = reports;
        this.loading = false;
      },
      error: (err: HttpErrorResponse) => {
        this.loading = false;
        this.error = err.error?.message ?? 'Could not load the non paying reports';
      },
    });
  }

  count(filter: Filter) {
    return filter === 'ALL' ? this.reports.length : this.reports.filter(r => r.status === filter).length;
  }

  get shown() {
    return this.filter === 'ALL' ? this.reports : this.reports.filter(r => r.status === this.filter);
  }

  /** How many reports a user has, to show repeat offenders. */
  reportsFor(userId: number) {
    return this.reports.filter(r => r.userId === userId).length;
  }

  initials(name: string | null) {
    return (name ?? '?').split(' ').filter(Boolean).slice(0, 2).map(p => p[0].toUpperCase()).join('') || '?';
  }

  openBan(report: NonPayingReport) {
    this.confirmBan = report;
    this.banReason = '';
  }

  ban(report: NonPayingReport) {
    const reason = this.banReason.trim();
    if (!reason) return;
    this.confirmBan = null;
    this.run(report, this.service.ban(report.id, reason), updated => {
      this.replace(updated);
      this.message = `${report.userName ?? 'The user'} is banned and can no longer log in or reserve.`;
    });
  }

  dismiss(report: NonPayingReport) {
    this.run(report, this.service.dismiss(report.id), updated => {
      this.replace([updated]);
      this.message = `Report on ${report.userName ?? 'the user'} dismissed. The user was not banned.`;
    });
  }

  unban(report: NonPayingReport) {
    this.run(report, this.service.unban(report.userId), () => {
      this.reports = this.reports.map(r => (r.userId === report.userId ? { ...r, userBlocked: false, banReason: null } : r));
      this.message = `${report.userName ?? 'The user'} is unbanned.`;
    });
  }

  private run<T>(report: NonPayingReport, action: Observable<T>, done: (result: T) => void) {
    this.busy = report.id;
    this.error = '';
    this.message = '';
    action.subscribe({
      next: result => {
        this.busy = null;
        done(result);
      },
      error: (err: HttpErrorResponse) => {
        this.busy = null;
        this.error = err.error?.message ?? 'The action failed';
      },
    });
  }

  /** Puts the updated reports in the list; a ban also blocks the user on their other reports. */
  private replace(updated: NonPayingReport[]) {
    const byId = new Map(updated.map(r => [r.id, r]));
    const users = new Map(updated.map(r => [r.userId, r]));
    this.reports = this.reports.map(r => {
      const next = byId.get(r.id) ?? r;
      const user = users.get(next.userId);
      return user ? { ...next, userBlocked: user.userBlocked, banReason: user.banReason } : next;
    });
  }
}
