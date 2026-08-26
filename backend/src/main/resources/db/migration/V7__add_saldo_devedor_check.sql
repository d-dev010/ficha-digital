-- M2: Restrição de saldo_devedor negativo
ALTER TABLE cliente ADD CONSTRAINT chk_saldo_nao_negativo CHECK (saldo_devedor >= 0);
