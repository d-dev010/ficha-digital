-- V11: Habilita unaccent e cria índice funcional para busca sem acento em nome (US04).
-- unaccent() remove acentos/diacríticos, permitindo que "e" encontre "é", "a" encontre "ã", etc.
-- O índice funcional garante que a busca continue eficiente mesmo com unaccent (RNF09).

CREATE EXTENSION IF NOT EXISTS unaccent;

-- Índice funcional em unaccent(lower(nome)) por farmácia — usado pela query de busca otimizada.
-- Substitui o índice simples em nome para suportar a busca sem acento com performance.
DROP INDEX IF EXISTS idx_cliente_farmacia_nome;
CREATE INDEX idx_cliente_farmacia_nome_unaccent
    ON cliente(farmacia_id, unaccent(lower(nome)));
