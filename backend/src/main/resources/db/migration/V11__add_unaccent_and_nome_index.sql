-- V11: Habilita unaccent e cria índice funcional para busca sem acento em nome (US04).
-- unaccent() remove acentos/diacríticos, permitindo que "e" encontre "é", "a" encontre "ã", etc.
-- O índice funcional garante que a busca continue eficiente mesmo com unaccent (RNF09).

CREATE EXTENSION IF NOT EXISTS unaccent;

-- unaccent() nativa é STABLE — índices funcionais exigem IMMUTABLE.
-- Criamos um wrapper que declara IMMUTABLE explicitamente (padrão recomendado pela doc do PostgreSQL).
-- public.unaccent: qualificamos o schema pois o search_path dentro de funções SQL pode não incluí-lo.
CREATE OR REPLACE FUNCTION unaccent_immutable(text)
    RETURNS text AS $$
        SELECT public.unaccent($1);
    $$ LANGUAGE SQL IMMUTABLE STRICT PARALLEL SAFE;

-- Índice funcional usando o wrapper — substitui o índice simples em nome.
-- Permite busca sem acento com performance para 5.000+ clientes (RNF09).
DROP INDEX IF EXISTS idx_cliente_farmacia_nome;
CREATE INDEX idx_cliente_farmacia_nome_unaccent
    ON cliente(farmacia_id, unaccent_immutable(lower(nome)));
