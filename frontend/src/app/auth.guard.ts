import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service'
import { map } from 'rxjs';

/**
 * Lets signed-in staff with one of [roles] in. Others go to the login page,
 * or to their own home page when they are signed in with another role.
 */
function roleGuard(roles: string[]): CanActivateFn {
  return (_route, state) => {
    const auth = inject(AuthService);
    const router = inject(Router);

    return auth.validateSession().pipe(
      map(valid => {
        if (!valid) return router.createUrlTree(['/login'], { queryParams: { returnUrl: state.url } });
        return roles.includes(auth.currentUser!.role) ? true : router.parseUrl(auth.homeUrl);
      })
    );
  };
}

/** Parking areas and the layout editor. */
export const authGuard: CanActivateFn = roleGuard(['ADMIN']);

/** SCRUM-88: the ticket counter page (admins may use it too). */
export const counterGuard: CanActivateFn = roleGuard(['TICKET_COUNTER', 'ADMIN']);
