-- =====================================================================
-- Who Wins · Banco de dados da tela "Criar grupo de desafio"
--
-- Fontes: diagrama de classes, protótipo da tela e UC "Criar grupo".
-- Escopo: apenas as entidades que a criação do grupo lê ou grava.
--
-- Mapa tela -> banco
--   Nome do grupo               -> grupo_desafio.nome
--   Atividade ou hábito         -> grupo_desafio.atividade
--   Categoria                   -> grupo_desafio.categoria_id (categoria_desafio)
--   Duração (qtd + unidade)     -> grupo_desafio.duracao_quantidade / duracao_unidade / data_fim
--   Critérios de pontuação      -> criterio_pontuacao.descricao
--   Forma de quantificação      -> criterio_pontuacao.unidade_medida (obrigatória p/ "Outro")
--   Visibilidade                -> grupo_desafio.visibilidade
--   Limite de participantes     -> grupo_desafio.limite_participantes (NULL = sem limite)
--   Premiação                   -> grupo_desafio.premiacao (NULL = sem premiação)
--   "Registrar criador e dono"  -> participacao (papel = 'DONO')
-- =====================================================================

BEGIN;

-- ---------------------------------------------------------------------
-- Tipos enumerados (enumerations do diagrama + os novos da tela)
-- ---------------------------------------------------------------------
CREATE TYPE visibilidade        AS ENUM ('PUBLICO', 'PRIVADO');
CREATE TYPE status_grupo        AS ENUM ('ATIVO', 'ENCERRADO');
CREATE TYPE status_participacao AS ENUM ('ATIVA', 'ENCERRADA', 'EXPULSA');
CREATE TYPE unidade_duracao     AS ENUM ('DIAS', 'SEMANAS', 'MESES');      -- select "durUn"
CREATE TYPE papel_participacao  AS ENUM ('DONO', 'PARTICIPANTE');

-- ---------------------------------------------------------------------
-- usuario
-- Só os atributos do diagrama; a tabela existe para o criador do grupo.
-- ---------------------------------------------------------------------
CREATE TABLE usuario (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nome_usuario  VARCHAR(50)  NOT NULL UNIQUE,
    email         VARCHAR(255) NOT NULL UNIQUE,
    senha_hash    VARCHAR(255) NOT NULL,          -- nunca guardar a senha em texto puro
    data_criacao  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- ---------------------------------------------------------------------
-- categoria_desafio
-- Alimenta o <select> de categorias. "Outro" é obrigatória (RN-001) e
-- exige a "forma de quantificação".
-- ---------------------------------------------------------------------
CREATE TABLE categoria_desafio (
    id                   SMALLINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nome                 VARCHAR(60) NOT NULL UNIQUE,
    exige_quantificacao  BOOLEAN     NOT NULL DEFAULT FALSE
);

-- Lista provisória do protótipo (aguarda confirmação do time)
INSERT INTO categoria_desafio (nome, exige_quantificacao) VALUES
    ('Exercício físico', FALSE),
    ('Estudos',          FALSE),
    ('Alimentação',      FALSE),
    ('Leitura',          FALSE),
    ('Bem-estar',        FALSE),
    ('Outro',            TRUE);

-- ---------------------------------------------------------------------
-- grupo_desafio
-- ---------------------------------------------------------------------
CREATE TABLE grupo_desafio (
    id                    BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    criador_id            BIGINT          NOT NULL REFERENCES usuario(id)           ON DELETE RESTRICT,
    categoria_id          SMALLINT        NOT NULL REFERENCES categoria_desafio(id) ON DELETE RESTRICT,

    nome                  VARCHAR(100)    NOT NULL,
    atividade             TEXT            NOT NULL,

    duracao_quantidade    INTEGER         NOT NULL,
    duracao_unidade       unidade_duracao NOT NULL DEFAULT 'DIAS',
    data_inicio           DATE            NOT NULL DEFAULT CURRENT_DATE,
    data_fim              DATE            NOT NULL,

    visibilidade          visibilidade    NOT NULL DEFAULT 'PUBLICO',
    status                status_grupo    NOT NULL DEFAULT 'ATIVO',
    limite_participantes  INTEGER,                      -- NULL = sem limite
    premiacao             TEXT,                         -- opcional; não existe no diagrama de classes
    link_convite          VARCHAR(64)     NOT NULL UNIQUE
                              DEFAULT replace(gen_random_uuid()::text, '-', ''),

    data_criacao          TIMESTAMPTZ     NOT NULL DEFAULT now(),
    data_atualizacao      TIMESTAMPTZ     NOT NULL DEFAULT now(),

    CONSTRAINT ck_grupo_nome_preenchido      CHECK (btrim(nome) <> ''),
    CONSTRAINT ck_grupo_atividade_preenchida CHECK (btrim(atividade) <> ''),
    CONSTRAINT ck_grupo_duracao_positiva     CHECK (duracao_quantidade >= 1),
    CONSTRAINT ck_grupo_limite_positivo      CHECK (limite_participantes IS NULL OR limite_participantes >= 1),
    CONSTRAINT ck_grupo_periodo_valido       CHECK (data_fim >= data_inicio)
);

CREATE INDEX ix_grupo_criador   ON grupo_desafio (criador_id);
CREATE INDEX ix_grupo_categoria ON grupo_desafio (categoria_id);
-- Busca de grupos públicos ativos
CREATE INDEX ix_grupo_descoberta ON grupo_desafio (data_criacao DESC)
    WHERE visibilidade = 'PUBLICO' AND status = 'ATIVO';

-- ---------------------------------------------------------------------
-- criterio_pontuacao   (grupo 1 --- 1 critério, composição)
-- ---------------------------------------------------------------------
CREATE TABLE criterio_pontuacao (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    grupo_id            BIGINT        NOT NULL UNIQUE REFERENCES grupo_desafio(id) ON DELETE CASCADE,
    descricao           TEXT          NOT NULL,          -- campo "Critérios de pontuação"
    unidade_medida      VARCHAR(100),                    -- campo "Forma de quantificação"
    pontos_por_unidade  NUMERIC(10,2),                   -- ainda não coletado pela tela

    CONSTRAINT ck_criterio_descricao_preenchida CHECK (btrim(descricao) <> ''),
    CONSTRAINT ck_criterio_pontos_positivos     CHECK (pontos_por_unidade IS NULL OR pontos_por_unidade > 0)
);

-- Regra da tela: se a categoria exige quantificação ("Outro"),
-- unidade_medida é obrigatória. Check entre tabelas só é possível via trigger.
CREATE FUNCTION fn_valida_quantificacao() RETURNS trigger
LANGUAGE plpgsql AS $$
DECLARE
    v_exige BOOLEAN;
BEGIN
    SELECT c.exige_quantificacao
      INTO v_exige
      FROM grupo_desafio g
      JOIN categoria_desafio c ON c.id = g.categoria_id
     WHERE g.id = NEW.grupo_id;

    IF v_exige AND btrim(COALESCE(NEW.unidade_medida, '')) = '' THEN
        RAISE EXCEPTION 'Informe como o progresso será medido (forma de quantificação).'
              USING ERRCODE = 'check_violation';
    END IF;
    RETURN NEW;
END $$;

CREATE TRIGGER tg_criterio_quantificacao
    BEFORE INSERT OR UPDATE ON criterio_pontuacao
    FOR EACH ROW EXECUTE FUNCTION fn_valida_quantificacao();

-- ---------------------------------------------------------------------
-- participacao   (usuario 1 --- 0..* participacao 0..* --- 1 grupo)
-- Na criação, o criador entra como DONO ("registrar usuário como criador e dono").
-- ---------------------------------------------------------------------
CREATE TABLE participacao (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    grupo_id         BIGINT              NOT NULL REFERENCES grupo_desafio(id) ON DELETE CASCADE,
    usuario_id       BIGINT              NOT NULL REFERENCES usuario(id)       ON DELETE CASCADE,
    data_entrada     DATE                NOT NULL DEFAULT CURRENT_DATE,
    papel            papel_participacao  NOT NULL DEFAULT 'PARTICIPANTE',
    pontuacao        NUMERIC(12,2)       NOT NULL DEFAULT 0,
    sequencia_atual  INTEGER             NOT NULL DEFAULT 0,
    maior_sequencia  INTEGER             NOT NULL DEFAULT 0,
    status           status_participacao NOT NULL DEFAULT 'ATIVA',

    CONSTRAINT uq_participacao_usuario_grupo UNIQUE (grupo_id, usuario_id),
    CONSTRAINT ck_participacao_sequencias    CHECK (sequencia_atual >= 0 AND maior_sequencia >= sequencia_atual)
);

CREATE INDEX ix_participacao_usuario ON participacao (usuario_id);
-- Cada grupo tem exatamente um dono
CREATE UNIQUE INDEX uq_grupo_um_dono ON participacao (grupo_id) WHERE papel = 'DONO';

-- ---------------------------------------------------------------------
-- criar_grupo_desafio(...)
-- Executa, em uma única transação, o que o diagrama de sequência descreve:
--   validar dados -> salvar grupo e configurações -> registrar criador como dono.
-- Retorna o id do grupo criado. Se qualquer passo falhar, nada é gravado.
-- ---------------------------------------------------------------------
CREATE FUNCTION criar_grupo_desafio(
    p_usuario_id          BIGINT,
    p_nome                VARCHAR,
    p_atividade           TEXT,
    p_categoria_id        SMALLINT,
    p_duracao_quantidade  INTEGER,
    p_duracao_unidade     unidade_duracao,
    p_criterios           TEXT,
    p_forma_quantificacao VARCHAR      DEFAULT NULL,
    p_visibilidade        visibilidade DEFAULT 'PUBLICO',
    p_limite              INTEGER      DEFAULT NULL,
    p_premiacao           TEXT         DEFAULT NULL
) RETURNS BIGINT
LANGUAGE plpgsql AS $$
DECLARE
    v_inicio  DATE := CURRENT_DATE;
    v_fim     DATE;
    v_exige   BOOLEAN;
    v_grupo   BIGINT;
BEGIN
    SELECT exige_quantificacao INTO v_exige
      FROM categoria_desafio WHERE id = p_categoria_id;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'Categoria inválida.' USING ERRCODE = 'foreign_key_violation';
    END IF;

    -- Fim = início + duração informada
    v_fim := (v_inicio + CASE p_duracao_unidade
                           WHEN 'DIAS'    THEN make_interval(days   => p_duracao_quantidade)
                           WHEN 'SEMANAS' THEN make_interval(weeks  => p_duracao_quantidade)
                           WHEN 'MESES'   THEN make_interval(months => p_duracao_quantidade)
                         END)::DATE;

    INSERT INTO grupo_desafio (
        criador_id, categoria_id, nome, atividade,
        duracao_quantidade, duracao_unidade, data_inicio, data_fim,
        visibilidade, limite_participantes, premiacao
    ) VALUES (
        p_usuario_id, p_categoria_id, btrim(p_nome), btrim(p_atividade),
        p_duracao_quantidade, p_duracao_unidade, v_inicio, v_fim,
        p_visibilidade, p_limite, NULLIF(btrim(COALESCE(p_premiacao, '')), '')
    ) RETURNING id INTO v_grupo;

    INSERT INTO criterio_pontuacao (grupo_id, descricao, unidade_medida)
    VALUES (
        v_grupo,
        btrim(p_criterios),
        CASE WHEN v_exige THEN btrim(p_forma_quantificacao) ELSE NULL END
    );

    INSERT INTO participacao (grupo_id, usuario_id, papel)
    VALUES (v_grupo, p_usuario_id, 'DONO');

    RETURN v_grupo;
END $$;

COMMIT;

-- ---------------------------------------------------------------------
-- Exemplo de uso (descomente para testar)
-- ---------------------------------------------------------------------
-- INSERT INTO usuario (nome_usuario, email, senha_hash)
-- VALUES ('maria', 'maria@exemplo.com', 'hash-de-exemplo');
--
-- SELECT criar_grupo_desafio(
--     1, 'Corrida de 30 dias', 'Correr pelo menos 3 km por dia',
--     (SELECT id FROM categoria_desafio WHERE nome = 'Exercício físico'),
--     30, 'DIAS', '1 ponto por dia cumprido; 2 pontos se passar de 5 km',
--     NULL, 'PUBLICO', 10, 'Quem ficar em 1º escolhe o restaurante'
-- );
