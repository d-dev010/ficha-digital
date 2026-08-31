-- Adiciona endereço ao cliente (opcional)
ALTER TABLE cliente ADD COLUMN endereco VARCHAR(255);

-- Adiciona quem retirou o item em cada lançamento (opcional)
ALTER TABLE lancamento ADD COLUMN pessoa_retirou VARCHAR(255);
