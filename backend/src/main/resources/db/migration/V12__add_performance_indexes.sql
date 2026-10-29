-- V12: Índices para otimização de performance (RNF08, RNF09)

-- 1. Índice composto para listagem alfabética de clientes ativos por farmácia
-- Acelera GET /clientes (listagem inicial) evitando seq scan e sort em memória
CREATE INDEX IF NOT EXISTS idx_cliente_farmacia_anonimizado_nome
    ON cliente(farmacia_id, anonimizado, nome);

-- 2. Índices por data em lançamentos e pagamentos
-- Acelera consultas e exportações de relatórios por período (sargable range scan)
CREATE INDEX IF NOT EXISTS idx_lancamento_data
    ON lancamento(data DESC);

CREATE INDEX IF NOT EXISTS idx_pagamento_data
    ON pagamento(data DESC);

