export interface RegistrarPagamentoRequest {
  valor: number;
}

export interface PagamentoResponse {
  id: string;
  clienteId: string;
  usuarioId: string;
  valor: number;
  data: string;
}

export type TipoMovimento = 'LANCAMENTO' | 'PAGAMENTO';

export interface ExtratoItem {
  id: string;
  tipo: TipoMovimento;
  valor: number;
  descricao: string | null;
  data: string;
  nomeResponsavel: string;
}
