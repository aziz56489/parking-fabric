import { Component, OnInit, inject } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { AdminReportsService, EarningsStats, EntryStats, Period } from '../admin-reports/admin-reports.service';
import { ParkingService } from '../parking.service';
import { ParkingArea } from '../models';
import { BarChartComponent, BarPoint } from './bar-chart.component';

/** SCRUM-81 (daily, monthly and yearly earnings) and SCRUM-80 (daily entries per parking area). */
@Component({
  selector: 'app-admin-stats',
  standalone: true,
  imports: [FormsModule, BarChartComponent],
  templateUrl: './admin-stats.component.html',
  styleUrl: './admin-stats.component.css',
})
export class AdminStatsComponent implements OnInit {
  private service = inject(AdminReportsService);
  private parking = inject(ParkingService);

  readonly periods: { value: Period; label: string; window: string }[] = [
    { value: 'DAY', label: 'Daily', window: 'last 30 days' },
    { value: 'MONTH', label: 'Monthly', window: 'last 12 months' },
    { value: 'YEAR', label: 'Yearly', window: 'last 5 years' },
  ];

  period: Period = 'DAY';
  earnings: EarningsStats | null = null;
  earningsError = '';

  areas: ParkingArea[] = [];
  /** null = all parking areas. */
  areaId: number | null = null;
  entries: EntryStats | null = null;
  entriesError = '';

  readonly money = (value: number) => this.formatMoney(value);
  readonly whole = (value: number) => (Number.isInteger(value) ? String(value) : value.toFixed(1));

  ngOnInit() {
    this.loadEarnings();
    this.loadEntries();
    this.parking.getParkingAreas().subscribe(areas => (this.areas = areas));
  }

  setPeriod(period: Period) {
    this.period = period;
    this.loadEarnings();
  }

  loadEarnings() {
    this.earningsError = '';
    this.service.getEarnings(this.period).subscribe({
      next: stats => (this.earnings = stats),
      error: (err: HttpErrorResponse) => (this.earningsError = err.error?.message ?? 'Could not load the earnings'),
    });
  }

  loadEntries() {
    this.entriesError = '';
    this.service.getEntries(this.areaId).subscribe({
      next: stats => (this.entries = stats),
      error: (err: HttpErrorResponse) => (this.entriesError = err.error?.message ?? 'Could not load the entries'),
    });
  }

  get periodWindow() {
    return this.periods.find(p => p.value === this.period)!.window;
  }

  get earningPoints(): BarPoint[] {
    return (this.earnings?.buckets ?? []).map(b => ({
      label: b.label,
      value: b.total,
      display: this.formatMoney(b.total),
      detail: `${b.exits} paid ${b.exits === 1 ? 'exit' : 'exits'}`,
    }));
  }

  get entryPoints(): BarPoint[] {
    return (this.entries?.daily ?? []).map(d => ({
      label: d.label,
      value: d.entries,
      display: `${d.entries} ${d.entries === 1 ? 'entry' : 'entries'}`,
    }));
  }

  get selectedAreaName() {
    return this.areaId === null ? 'all parking areas' : (this.areas.find(a => a.id === this.areaId)?.name ?? 'this area');
  }

  formatMoney(value: number) {
    const currency = this.earnings?.currency ?? 'TND';
    try {
      return new Intl.NumberFormat('en', { style: 'currency', currency, currencyDisplay: 'code' }).format(value);
    } catch {
      return `${value.toFixed(2)} ${currency}`;
    }
  }
}
