import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { catchError, map, Observable, of, switchMap, tap } from 'rxjs';

export interface User {
  id: number;
  name: string;
  email: string;
  role: string;
  isHandicapped: boolean;
  isEvOwner: boolean;
  isBlocked: boolean;
}

export interface LoginRequest {
  email: string;
  password: string;
}

const STORAGE_KEY = 'parking_admin_user';
const API_BASE = 'http://localhost:8080/api';

/** Accounts allowed in this web app. Drivers (USER) use the mobile app. */
const STAFF_ROLES = ['ADMIN', 'TICKET_COUNTER'];

@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);

  private _currentUser: User | null = null;

  constructor() {
    const stored = sessionStorage.getItem(STORAGE_KEY);
    if (stored) {
      try {
        this._currentUser = JSON.parse(stored) as User;
      } catch {
        this._currentUser = null;
      }
    }
  }

  get currentUser(): User | null {
    return this._currentUser;
  }

  get isLoggedIn(): boolean {
    return this._currentUser !== null;
  }

  get isTicketCounter(): boolean {
    return this._currentUser?.role === 'TICKET_COUNTER';
  }

  /** Where a user lands after signing in: admins manage areas, ticket counters work at their desk. */
  get homeUrl(): string {
    return this.isTicketCounter ? '/counter' : '/';
  }

  get isAdmin(): boolean {
    return this._currentUser?.role === 'ADMIN';
  }

  login(email: string, password: string): Observable<User> {
    return this.http.get(`${API_BASE}/auth/csrf`).pipe(
      switchMap(() => this.http.post<User>(`${API_BASE}/auth/login`, { email, password })),
      tap(user => {
        if (!STAFF_ROLES.includes(user.role)) {
          throw new Error('Access denied: staff accounts only');
        }
        this._currentUser = user;
        sessionStorage.setItem(STORAGE_KEY, JSON.stringify(user));
      })
    );
  }

  validateSession(): Observable<boolean> {
    if (!this._currentUser || !STAFF_ROLES.includes(this._currentUser.role)) return of(false);
    return this.http.get<User>(`${API_BASE}/auth/me`).pipe(
      tap(user => {
        this._currentUser = user;
        sessionStorage.setItem(STORAGE_KEY, JSON.stringify(user));
      }),
      map(() => true),
      catchError(() => {
        this._currentUser = null;
        sessionStorage.removeItem(STORAGE_KEY);
        return of(false);
      })
    );
  }

  logout(): void {
    this.http.get(`${API_BASE}/auth/csrf`).pipe(
      switchMap(() => this.http.post<void>(`${API_BASE}/auth/logout`, {})),
      catchError(() => of(undefined))
    ).subscribe(() => {
      this._currentUser = null;
      sessionStorage.removeItem(STORAGE_KEY);
    });
  }
}
