export type Perfil = 'DONO' | 'ATENDENTE';

export interface TokenResponse {
  token: string;
  usuarioId: string;
  nome: string;
  perfil: Perfil;
  farmaciaId: string;
}

export interface UsuarioAutenticado {
  usuarioId: string;
  nome: string;
  perfil: Perfil;
  farmaciaId: string;
}
