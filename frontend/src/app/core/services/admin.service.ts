import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { FarmaciaAdmin, UsuarioAdmin, Perfil } from '../models/usuario.model';

const API = environment.apiUrl;

export interface CriarFarmaciaAdminRequest {
  nomeFarmacia: string;
  cnpj?: string;
  nomeDono: string;
  emailDono: string;
  senhaDono: string;
}

export interface CriarUsuarioAdminRequest {
  nome: string;
  email: string;
  senha: string;
  perfil: Perfil;
  farmaciaId: string;
}

@Injectable({ providedIn: 'root' })
export class AdminService {
  constructor(private http: HttpClient) {}

  // ─── Farmácias ────────────────────────────────────────────────

  listarFarmacias(): Observable<FarmaciaAdmin[]> {
    return this.http.get<FarmaciaAdmin[]>(`${API}/admin/farmacias`);
  }

  criarFarmacia(req: CriarFarmaciaAdminRequest): Observable<FarmaciaAdmin> {
    return this.http.post<FarmaciaAdmin>(`${API}/admin/farmacias`, req);
  }

  excluirFarmacia(id: string): Observable<void> {
    return this.http.delete<void>(`${API}/admin/farmacias/${id}`);
  }

  // ─── Usuários ─────────────────────────────────────────────────

  listarUsuarios(farmaciaId?: string): Observable<UsuarioAdmin[]> {
    const params: Record<string, string> = farmaciaId ? { farmaciaId } : {};
    return this.http.get<UsuarioAdmin[]>(`${API}/admin/usuarios`, { params });
  }

  criarUsuario(req: CriarUsuarioAdminRequest): Observable<UsuarioAdmin> {
    return this.http.post<UsuarioAdmin>(`${API}/admin/usuarios`, req);
  }

  alterarSenha(usuarioId: string, novaSenha: string): Observable<void> {
    return this.http.patch<void>(`${API}/admin/usuarios/${usuarioId}/senha`, { novaSenha });
  }

  alterarStatus(usuarioId: string, ativo: boolean): Observable<UsuarioAdmin> {
    return this.http.patch<UsuarioAdmin>(`${API}/admin/usuarios/${usuarioId}/status`, { ativo });
  }

  excluirUsuario(id: string): Observable<void> {
    return this.http.delete<void>(`${API}/admin/usuarios/${id}`);
  }
}
