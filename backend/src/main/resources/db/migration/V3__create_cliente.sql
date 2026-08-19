-- V3: Cria tabela cliente
-- saldo_devedor mantido de forma transacional (RNF10).
-- CPF armazenado completo; mascarado na camada de aplicação nas listagens (RNF04).
-- Índices compostos em nome/telefone/cpf por farmácia para performance com 5.000+ clientes (RNF09).

CREATE TABLE cliente (
    id            UUID           NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    farmacia_id   UUID           NOT NULL REFERENCES farmacia(id) ON DELETE CASCADE,
    nome          VARCHAR(255)   NOT NULL,
    telefone      VARCHAR(20),
    cpf           VARCHAR(14),
    saldo_devedor NUMERIC(10, 2) NOT NULL DEFAULT 0.00,

    CONSTRAINT ck_cliente_saldo_nao_negativo CHECK (saldo_devedor >= 0)
);

-- Índices compostos por farmácia para busca (US04) e performance (RNF09)
CREATE INDEX idx_cliente_farmacia_nome     ON cliente(farmacia_id, nome);
CREATE INDEX idx_cliente_farmacia_telefone ON cliente(farmacia_id, telefone);
CREATE INDEX idx_cliente_farmacia_cpf      ON cliente(farmacia_id, cpf);
