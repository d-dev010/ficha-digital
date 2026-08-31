import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

const API_URL = 'http://localhost:8080';

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

@Injectable({ providedIn: 'root' })
export class FuncionariosService {
  constructor(private http: HttpClient) {}

  cadastrar(request: CadastrarFuncionarioRequest): Observable<UsuarioResponse> {
    return this.http.post<UsuarioResponse>(`${API_URL}/usuarios`, request);
  }
}
