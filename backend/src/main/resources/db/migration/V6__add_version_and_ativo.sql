-- Migration V6: Adiciona colunas para Optimistic Locking (Problema 3) e Revogação de JWT (Problema 5)

-- 1. Suporte a Optimistic Locking em cliente
ALTER TABLE cliente ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

-- 2. Suporte a status ativo/inativo para revogação instantânea de sessões
ALTER TABLE usuario ADD COLUMN ativo BOOLEAN NOT NULL DEFAULT TRUE;
