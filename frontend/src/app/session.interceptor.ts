import { HttpInterceptorFn } from '@angular/common/http';

const API_ORIGIN = 'http://localhost:8080';
const SAFE_METHODS = new Set(['GET', 'HEAD', 'OPTIONS', 'TRACE']);

function csrfCookie(): string | null {
  if (typeof document === 'undefined') return null;
  const cookie = document.cookie
    .split('; ')
    .find(value => value.startsWith('XSRF-TOKEN='));
  return cookie ? decodeURIComponent(cookie.substring('XSRF-TOKEN='.length)) : null;
}

export const sessionInterceptor: HttpInterceptorFn = (request, next) => {
  if (!request.url.startsWith(`${API_ORIGIN}/api/`)) return next(request);

  let outgoing = request.clone({ withCredentials: true });
  if (!SAFE_METHODS.has(request.method.toUpperCase())) {
    const token = csrfCookie();
    if (token) outgoing = outgoing.clone({ setHeaders: { 'X-XSRF-TOKEN': token } });
  }
  return next(outgoing);
};
