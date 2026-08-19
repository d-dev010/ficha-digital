-- V4: Cria tabela lancamento
-- Registra fiado lançado para o cliente.
-- usuario_id rastreia quem lançou (US05).
-- valor deve ser positivo.
-- Índice em (cliente_id, data DESC) para extrato eficiente (RNF08/RNF09).

CREATE TABLE lancamento (
    id          UUID           NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    cliente_id  UUID           NOT NULL REFERENCES cliente(id) ON DELETE CASCADE,
    usuario_id  UUID           NOT NULL REFERENCES usuario(id),
    valor       NUMERIC(10, 2) NOT NULL,
    descricao   VARCHAR(500),
    data        TIMESTAMPTZ    NOT NULL DEFAULT NOW(),

    CONSTRAINT ck_lancamento_valor_positivo CHECK (valor > 0)
);

CREATE INDEX idx_lancamento_cliente_data ON lancamento(cliente_id, data DESC);
