import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { AuthService } from './auth.service';

/**
 * Interceptor JWT:
 * 1. Injeta "Authorization: Bearer <token>" em toda requisição autenticada.
 * 2. Se a API retornar 401, faz logout automático (token expirado/inválido).
 */
export const jwtInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const token = auth.getToken();

  const authReq = token
    ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
    : req;

  return next(authReq).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401) {
        auth.logout(); // Token expirado — desloga e redireciona para /login
      }
      return throwError(() => error);
    })
  );
};
