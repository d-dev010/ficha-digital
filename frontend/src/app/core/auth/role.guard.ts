import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';

/**
 * Guard de perfil — impede que ATENDENTE acesse rotas exclusivas de DONO.
 * Uso: canActivate: [authGuard, roleGuard('DONO')]
 */
export const roleGuard = (perfilRequerido: 'DONO' | 'ATENDENTE'): CanActivateFn => {
  return () => {
    const auth = inject(AuthService);
    const router = inject(Router);
    if (auth.perfil() === perfilRequerido || perfilRequerido === 'ATENDENTE') return true;
    // ATENDENTE tentando acessar rota de DONO → redireciona para busca
    return router.createUrlTree(['/clientes']);
  };
};
