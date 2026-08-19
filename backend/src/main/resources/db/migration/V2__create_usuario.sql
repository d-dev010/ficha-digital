-- V2: Cria tabela usuario
-- Usuário pertence a uma farmácia (FK farmacia_id).
-- Senha armazenada sempre como hash bcrypt — nunca texto puro (RNF01).
-- Perfil: DONO ou ATENDENTE.

CREATE TABLE usuario (
    id          UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    farmacia_id UUID         NOT NULL REFERENCES farmacia(id) ON DELETE CASCADE,
    nome        VARCHAR(255) NOT NULL,
    email       VARCHAR(255) NOT NULL UNIQUE,
    senha_hash  VARCHAR(255) NOT NULL,
    perfil      VARCHAR(10)  NOT NULL CHECK (perfil IN ('DONO', 'ATENDENTE'))
);

CREATE INDEX idx_usuario_farmacia ON usuario(farmacia_id);
