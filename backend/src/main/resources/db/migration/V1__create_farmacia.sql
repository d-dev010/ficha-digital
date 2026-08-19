-- V1: Cria tabela farmacia
-- Multi-tenant root: todo dado é isolado por farmacia_id (RNF03)

CREATE TABLE farmacia (
    id   UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    nome VARCHAR(255) NOT NULL,
    cnpj VARCHAR(14)  UNIQUE
);
