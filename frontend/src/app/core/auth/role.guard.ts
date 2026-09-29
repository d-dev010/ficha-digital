import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';
import { Perfil } from '../models/usuario.model';

/**
 * Guard de perfil — impede acesso a rotas com perfil insuficiente.
 * SUPER_ADMIN sempre tem acesso irrestrito.
 * Uso: canActivate: [authGuard, roleGuard('DONO')]
 */
export const roleGuard = (perfilRequerido: Perfil): CanActivateFn => {
  return () => {
    const auth = inject(AuthService);
    const router = inject(Router);
    const perfil = auth.perfil();

    // SUPER_ADMIN tem acesso a tudo
    if (perfil === 'SUPER_ADMIN') return true;
    // Perfil suficiente
    if (perfil === perfilRequerido) return true;
    // DONO pode acessar rotas de ATENDENTE
    if (perfilRequerido === 'ATENDENTE' && perfil === 'DONO') return true;

    return router.createUrlTree(['/clientes']);
  };
};

