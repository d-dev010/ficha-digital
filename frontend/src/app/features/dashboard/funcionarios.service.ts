import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';

const API_URL = environment.apiUrl;

export interface CadastrarFuncionarioRequest {
  nome: string;
  email: string;
  senhaTemporaria: string;
}

export interface UsuarioResponse {
  id: string;
  nome: string;
  email: string;
  perfil: string;
}

export interface ResumoFarmacia {
  clientesAtivos: number;
  totalAReceber: number;
}

@Injectable({ providedIn: 'root' })
export class FuncionariosService {
  constructor(private http: HttpClient) {}

  cadastrar(request: CadastrarFuncionarioRequest): Observable<UsuarioResponse> {
    return this.http.post<UsuarioResponse>(`${API_URL}/usuarios`, request);
  }

  resumo(): Observable<ResumoFarmacia> {
    return this.http.get<ResumoFarmacia>(`${API_URL}/farmacias/resumo`);
  }
}
