import { Injectable, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { tap } from 'rxjs/operators';
import { TokenResponse, UsuarioAutenticado } from '../models/usuario.model';

const TOKEN_KEY = 'fd_token';
const API_URL = 'http://localhost:8080';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private _usuario = signal<UsuarioAutenticado | null>(this.carregarDoStorage());

  /** Usuário autenticado atual (signal reativo). */
  readonly usuario = this._usuario.asReadonly();

  /** true se há sessão ativa. */
  readonly isAutenticado = computed(() => this._usuario() !== null);

  /** Perfil do usuário autenticado ('DONO' | 'ATENDENTE' | null). */
  readonly perfil = computed(() => this._usuario()?.perfil ?? null);

  constructor(private http: HttpClient, private router: Router) {}

  login(email: string, senha: string) {
    return this.http.post<TokenResponse>(`${API_URL}/auth/login`, { email, senha }).pipe(
      tap(resp => {
        localStorage.setItem(TOKEN_KEY, resp.token);
        this._usuario.set({
          usuarioId: resp.usuarioId,
          nome: resp.nome,
          perfil: resp.perfil,
          farmaciaId: resp.farmaciaId,
        });
      })
    );
  }

  logout() {
    localStorage.removeItem(TOKEN_KEY);
    this._usuario.set(null);
    this.router.navigate(['/login']);
  }

  getToken(): string | null {
    return localStorage.getItem(TOKEN_KEY);
  }

  private carregarDoStorage(): UsuarioAutenticado | null {
    const token = localStorage.getItem(TOKEN_KEY);
    if (!token) return null;
    try {
      // Decodifica o payload do JWT (sem verificar assinatura — só para leitura local)
      const payload = JSON.parse(atob(token.split('.')[1]));
      // Verifica se o token está expirado
      if (payload.exp && Date.now() / 1000 > payload.exp) {
        localStorage.removeItem(TOKEN_KEY);
        return null;
      }
      return {
        usuarioId: payload.sub,
        nome: payload.nome,
        perfil: payload.perfil,
        farmaciaId: payload.farmaciaId,
      };
    } catch {
      localStorage.removeItem(TOKEN_KEY);
      return null;
    }
  }
}
