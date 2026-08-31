export interface ClienteResumo {
  id: string;
  nome: string;
  telefone: string | null;
  cpfMascarado: string | null;
  endereco: string | null;
  saldoDevedor: number;
}

export interface ClienteDetalhe {
  id: string;
  nome: string;
  telefone: string | null;
  cpf: string | null;
  endereco: string | null;
  saldoDevedor: number;
}

export interface CadastrarClienteRequest {
  nome: string;
  telefone?: string;
  cpf?: string;
  endereco?: string;
}

// Resposta paginada do Spring
export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}
