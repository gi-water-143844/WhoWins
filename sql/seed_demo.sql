-- Usuário fictício usado somente para demonstrar a criação de grupos.
-- A tela fornecida não possui fluxo de cadastro/login.
INSERT INTO usuario (nome_usuario, email, senha_hash)
VALUES ('usuario_demo', 'demo@whowins.local', 'HASH_NAO_UTILIZADO_NO_MODO_DEMO')
ON CONFLICT (email) DO NOTHING;
