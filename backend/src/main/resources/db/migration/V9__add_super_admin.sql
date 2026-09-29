-- V9: Suporte ao perfil SUPER_ADMIN (administrador global sem vínculo com farmácia)

-- 1. Tornar farmacia_id nullable para o SUPER_ADMIN (que não pertence a nenhuma farmácia)
ALTER TABLE usuario ALTER COLUMN farmacia_id DROP NOT NULL;

-- 2. Ampliar a coluna perfil de VARCHAR(10) para VARCHAR(15) para caber 'SUPER_ADMIN' (11 chars)
ALTER TABLE usuario ALTER COLUMN perfil TYPE VARCHAR(15);

-- 3. Atualizar o check constraint do perfil para incluir SUPER_ADMIN
ALTER TABLE usuario DROP CONSTRAINT IF EXISTS usuario_perfil_check;
ALTER TABLE usuario ADD CONSTRAINT usuario_perfil_check
    CHECK (perfil IN ('SUPER_ADMIN', 'DONO', 'ATENDENTE'));

-- 3. Criar o usuário SUPER_ADMIN a partir das variáveis de ambiente do sistema
--    ADMIN_EMAIL e ADMIN_PASSWORD_HASH devem ser configuradas antes do deploy.
--    Para gerar o hash BCrypt: use um gerador online ou o próprio Spring.
--    Exemplo de hash para "admin123": $2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM usuario WHERE perfil = 'SUPER_ADMIN'
    ) THEN
        INSERT INTO usuario (id, farmacia_id, nome, email, senha_hash, perfil, ativo)
        VALUES (
            gen_random_uuid(),
            NULL,
            'Super Admin',
            current_setting('app.admin_email', true),
            current_setting('app.admin_password_hash', true),
            'SUPER_ADMIN',
            TRUE
        );
    END IF;
END $$;
