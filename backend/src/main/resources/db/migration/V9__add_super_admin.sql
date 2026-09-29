-- V9: Suporte ao perfil SUPER_ADMIN (administrador global sem vínculo com farmácia)

-- 1. Tornar farmacia_id nullable para o SUPER_ADMIN (que não pertence a nenhuma farmácia)
ALTER TABLE usuario ALTER COLUMN farmacia_id DROP NOT NULL;

-- 2. Ampliar a coluna perfil de VARCHAR(10) para VARCHAR(15) para caber 'SUPER_ADMIN' (11 chars)
ALTER TABLE usuario ALTER COLUMN perfil TYPE VARCHAR(15);

-- 3. Atualizar o check constraint do perfil para incluir SUPER_ADMIN
ALTER TABLE usuario DROP CONSTRAINT IF EXISTS usuario_perfil_check;
ALTER TABLE usuario ADD CONSTRAINT usuario_perfil_check
    CHECK (perfil IN ('SUPER_ADMIN', 'DONO', 'ATENDENTE'));

