import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { AuthService } from "../auth.service";

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './login.component.html',
  styleUrl: './login.component.css',
})
export class LoginComponent {
  private auth = inject(AuthService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);

  email = '';
  password = '';
  error = '';
  loading = false;

  submit() {
    this.error = '';

    if (!this.email.trim() || !this.password) {
      this.error = 'Please enter your email and password.';
      return;
    }

    this.loading = true;
    this.auth.login(this.email.trim(), this.password).subscribe({
      next: () => {
        this.loading = false;
        const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl');
        this.router.navigateByUrl(returnUrl?.startsWith('/') ? returnUrl : this.auth.homeUrl);
      },
      error: (err: unknown) => {
        this.loading = false;
        if (err instanceof Error && err.message.startsWith('Access denied')) {
          this.error = 'Access denied. Only administrator and ticket counter accounts can access this application.';
        } else if (err instanceof HttpErrorResponse) {
          if (err.status === 0) {
            this.error = 'Cannot reach the server. Make sure the backend is running.';
          } else if (err.status === 401) {
            this.error = 'Invalid email or password.';
          } else if (err.status === 403) {
            this.error = err.error?.message ?? 'Account is blocked or access is forbidden.';
          } else {
            this.error = err.error?.message ?? 'Login failed. Please try again.';
          }
        } else {
          this.error = 'Login failed. Please try again.';
        }
      },
    });
  }
}
