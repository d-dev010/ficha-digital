import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

const API_URL = environment.apiUrl;

/**
 * Service para exportação de relatórios CSV.
 *
 * csvCliente → GET /clientes/{id}/extrato/csv
 * csvGeral   → GET /relatorio/csv
 *
 * Ambos aceitam mes + ano opcionais para filtro mensal.
 * O método download() aciona o download no browser via <a> temporário.
 */
@Injectable({ providedIn: 'root' })
export class RelatorioService {
  constructor(private http: HttpClient) {}

  /**
   * Exporta o extrato de um cliente específico.
   * @param clienteId UUID do cliente
   * @param mes       Mês (1–12) — omitir para histórico completo
   * @param ano       Ano (ex: 2026) — omitir para histórico completo
   */
  csvCliente(clienteId: string, mes?: number, ano?: number): Observable<Blob> {
    let params = new HttpParams();
    if (mes != null && ano != null) {
      params = params.set('mes', mes).set('ano', ano);
    }
    return this.http.get(`${API_URL}/clientes/${clienteId}/extrato/csv`, {
      params,
      responseType: 'blob',
    });
  }

  /**
   * Exporta o relatório geral de toda a farmácia (restrito ao DONO).
   * @param mes  Mês (1–12) — omitir para total
   * @param ano  Ano (ex: 2026) — omitir para total
   */
  csvGeral(mes?: number, ano?: number): Observable<Blob> {
    let params = new HttpParams();
    if (mes != null && ano != null) {
      params = params.set('mes', mes).set('ano', ano);
    }
    return this.http.get(`${API_URL}/relatorio/csv`, {
      params,
      responseType: 'blob',
    });
  }

  /**
   * Aciona o download de um Blob no browser criando um <a> temporário.
   * @param blob     Conteúdo do arquivo
   * @param filename Nome do arquivo para download
   */
  download(blob: Blob, filename: string): void {
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    a.click();
    setTimeout(() => URL.revokeObjectURL(url), 10_000);
  }
}
