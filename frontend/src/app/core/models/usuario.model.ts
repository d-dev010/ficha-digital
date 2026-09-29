export type Perfil = 'SUPER_ADMIN' | 'DONO' | 'ATENDENTE';

export interface TokenResponse {
  token: string;
  usuarioId: string;
  nome: string;
  perfil: Perfil;
  farmaciaId?: string;
}

export interface UsuarioAutenticado {
  usuarioId: string;
  nome: string;
  perfil: Perfil;
  farmaciaId?: string;
}

export interface FarmaciaAdmin {
  id: string;
  nome: string;
  cnpj: string;
  totalUsuarios: number;
}

export interface UsuarioAdmin {
  id: string;
  nome: string;
  email: string;
  perfil: Perfil;
  ativo: boolean;
  farmaciaId?: string;
  farmaciaNome?: string;
}
