import { Component, OnInit, inject } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { AdminAppeal, AdminReportsService, AppealStatus } from '../admin-reports/admin-reports.service';

type Filter = AppealStatus | 'ALL';

/** Admin dashboard panel: banned users' appeals. Accepting unbans the user. */
@Component({
  selector: 'app-admin-appeals',
  standalone: true,
  imports: [DatePipe, FormsModule],
  templateUrl: './admin-appeals.component.html',
  styleUrl: '../admin-reports/admin-reports.component.css',
})
export class AdminAppealsComponent implements OnInit {
  private service = inject(AdminReportsService);

  appeals: AdminAppeal[] = [];
  filter: Filter = 'PENDING';
  loading = true;
  error = '';
  message = '';
  busy: number | null = null;
  /** The admin's optional answer per appeal, shown to the user. */
  responses: Record<number, string> = {};

  readonly filters: { value: Filter; label: string }[] = [
    { value: 'PENDING', label: 'To review' },
    { value: 'ACCEPTED', label: 'Accepted' },
    { value: 'REJECTED', label: 'Rejected' },
    { value: 'ALL', label: 'All' },
  ];

  ngOnInit() {
    this.load();
  }

  load() {
    this.loading = true;
    this.service.getAppeals().subscribe({
      next: appeals => {
        this.appeals = appeals;
        this.loading = false;
      },
      error: (err: HttpErrorResponse) => {
        this.loading = false;
        this.error = err.error?.message ?? 'Could not load the appeals';
      },
    });
  }

  count(filter: Filter) {
    return filter === 'ALL' ? this.appeals.length : this.appeals.filter(a => a.status === filter).length;
  }

  get shown() {
    return this.filter === 'ALL' ? this.appeals : this.appeals.filter(a => a.status === this.filter);
  }

  initials(name: string | null) {
    return (name ?? '?').split(' ').filter(Boolean).slice(0, 2).map(p => p[0].toUpperCase()).join('') || '?';
  }

  decide(appeal: AdminAppeal, decision: 'accept' | 'reject') {
    this.busy = appeal.id;
    this.error = '';
    this.message = '';
    this.service.decideAppeal(appeal.id, decision, (this.responses[appeal.id] ?? '').trim()).subscribe({
      next: updated => {
        this.busy = null;
        this.appeals = this.appeals.map(a => (a.id === updated.id ? updated : a));
        this.message = decision === 'accept'
          ? `${appeal.userName ?? 'The user'} is unbanned and can use the app again.`
          : `Appeal rejected. ${appeal.userName ?? 'The user'} stays banned and can see your answer.`;
      },
      error: (err: HttpErrorResponse) => {
        this.busy = null;
        this.error = err.error?.message ?? 'The action failed';
      },
    });
  }
}
