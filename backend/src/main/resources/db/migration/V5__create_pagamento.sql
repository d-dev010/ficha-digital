-- V5: Cria tabela pagamento
-- Registra pagamento (total ou parcial) realizado pelo cliente.
-- usuario_id rastreia quem registrou (US07).
-- valor deve ser positivo.
-- Índice em (cliente_id, data DESC) para extrato eficiente (RNF08/RNF09).

CREATE TABLE pagamento (
    id          UUID           NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    cliente_id  UUID           NOT NULL REFERENCES cliente(id) ON DELETE CASCADE,
    usuario_id  UUID           NOT NULL REFERENCES usuario(id),
    valor       NUMERIC(10, 2) NOT NULL,
    data        TIMESTAMPTZ    NOT NULL DEFAULT NOW(),

    CONSTRAINT ck_pagamento_valor_positivo CHECK (valor > 0)
);

CREATE INDEX idx_pagamento_cliente_data ON pagamento(cliente_id, data DESC);
