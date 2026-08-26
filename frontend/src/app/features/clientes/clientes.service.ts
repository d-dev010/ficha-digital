import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  ClienteResumo, ClienteDetalhe, CadastrarClienteRequest, Page
} from '../../core/models/cliente.model';
import { LancarFiadoRequest, LancamentoResponse } from '../../core/models/lancamento.model';
import { RegistrarPagamentoRequest, PagamentoResponse, ExtratoItem } from '../../core/models/pagamento.model';

const API_URL = 'http://localhost:8080';

@Injectable({ providedIn: 'root' })
export class ClientesService {
  constructor(private http: HttpClient) {}

  buscar(termo: string, page = 0, size = 20): Observable<Page<ClienteResumo>> {
    const params = new HttpParams()
      .set('busca', termo)
      .set('page', page)
      .set('size', size);
    return this.http.get<Page<ClienteResumo>>(`${API_URL}/clientes`, { params });
  }

  detalhe(id: string): Observable<ClienteDetalhe> {
    return this.http.get<ClienteDetalhe>(`${API_URL}/clientes/${id}`);
  }

  cadastrar(request: CadastrarClienteRequest): Observable<ClienteDetalhe> {
    return this.http.post<ClienteDetalhe>(`${API_URL}/clientes`, request);
  }

  lancarFiado(clienteId: string, request: LancarFiadoRequest): Observable<LancamentoResponse> {
    return this.http.post<LancamentoResponse>(
      `${API_URL}/clientes/${clienteId}/lancamentos`, request
    );
  }

  registrarPagamento(clienteId: string, request: RegistrarPagamentoRequest): Observable<PagamentoResponse> {
    return this.http.post<PagamentoResponse>(
      `${API_URL}/clientes/${clienteId}/pagamentos`, request
    );
  }

  extrato(clienteId: string, page = 0, size = 50): Observable<Page<ExtratoItem>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<Page<ExtratoItem>>(
      `${API_URL}/clientes/${clienteId}/extrato`, { params }
    );
  }
}
