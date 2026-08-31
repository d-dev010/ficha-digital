export interface LancarFiadoRequest {
  valor: number;
  descricao: string;
  pessoaRetirou?: string;
}

export interface LancamentoResponse {
  id: string;
  clienteId: string;
  usuarioId: string;
  valor: number;
  descricao: string;
  data: string;
}
