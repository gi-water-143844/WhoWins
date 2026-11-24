const path = require("path");
require("dotenv").config({ path: path.join(__dirname, ".env") });

if (!process.env.DATABASE_URL) {
  throw new Error("DATABASE_URL não definido. Configure WhoWins/.env com a conexão do PostgreSQL/Supabase.");
}

const express = require("express");
const { Pool } = require("pg");

const app = express();
const port = Number(process.env.PORT || 3000);
const demoUserId = Number(process.env.DEMO_USER_ID || 1);

const pool = new Pool({
  connectionString: process.env.DATABASE_URL,
  ssl: {
    rejectUnauthorized: false
  }
});

app.use(express.json({ limit: "100kb" }));
app.use(express.static(path.join(__dirname, "public")));

const unidades = {
  dias: "DIAS",
  semanas: "SEMANAS",
  meses: "MESES"
};
const visibilidades = {
  publico: "PUBLICO",
  privado: "PRIVADO"
};

function formatarGrupo(g) {
  return {
    id: g.id,
    nome: g.nome,
    atividade: g.atividade,
    categoria: g.categoria,
    duracao: {
      quantidade: g.duracao_quantidade,
      unidade: g.duracao_unidade.toLowerCase()
    },
    criteriosPontuacao: g.criterios_pontuacao,
    formaQuantificacao: g.forma_quantificacao,
    visibilidade: g.visibilidade.toLowerCase(),
    limiteParticipantes: g.limite_participantes,
    premiacao: g.premiacao,
    dataInicio: g.data_inicio,
    dataFim: g.data_fim,
    linkConvite: g.link_convite
  };
}

async function carregarGrupo(client, grupoId) {
  const { rows } = await client.query(
    `SELECT g.id, g.nome, g.atividade, c.nome AS categoria,
            g.duracao_quantidade, lower(g.duracao_unidade::text) AS duracao_unidade,
            cp.descricao AS criterios_pontuacao,
            cp.unidade_medida AS forma_quantificacao,
            lower(g.visibilidade::text) AS visibilidade,
            g.limite_participantes, g.premiacao,
            g.data_inicio, g.data_fim, g.link_convite
       FROM grupo_desafio g
       JOIN categoria_desafio c ON c.id = g.categoria_id
       JOIN criterio_pontuacao cp ON cp.grupo_id = g.id
      WHERE g.id = $1`,
    [grupoId]
  );
  return rows[0] || null;
}

function validarGrupo(d) {
  const erros = [];
  if (!d || typeof d !== "object") return ["Envie os dados do grupo em JSON."];
  if (typeof d.nome !== "string" || !d.nome.trim()) erros.push("Informe o nome do grupo.");
  else if (d.nome.trim().length > 100) erros.push("O nome do grupo deve ter no máximo 100 caracteres.");
  if (typeof d.atividade !== "string" || !d.atividade.trim()) erros.push("Descreva a atividade ou o hábito.");
  if (typeof d.categoria !== "string" || !d.categoria.trim()) erros.push("Selecione uma categoria.");
  if (!d.duracao || !Number.isInteger(Number(d.duracao.quantidade)) || Number(d.duracao.quantidade) < 1)
    erros.push("A duração deve ser um número inteiro maior que zero.");
  if (!d.duracao || !unidades[d.duracao.unidade]) erros.push("Unidade de duração inválida.");
  if (typeof d.criteriosPontuacao !== "string" || !d.criteriosPontuacao.trim())
    erros.push("Informe os critérios de pontuação.");
  if (d.visibilidade && !visibilidades[d.visibilidade]) erros.push("Visibilidade inválida.");
  if (d.limiteParticipantes !== null && d.limiteParticipantes !== undefined &&
      (!Number.isInteger(Number(d.limiteParticipantes)) || Number(d.limiteParticipantes) < 1))
    erros.push("O limite deve ser um inteiro positivo ou ficar vazio.");
  if (d.premiacao != null && typeof d.premiacao !== "string") erros.push("Premiação inválida.");
  return erros;
}

app.get("/api/health", async (_req, res) => {
  try {
    await pool.query("SELECT 1");
    res.json({ status: "ok", banco: "conectado" });
  } catch (err) {
    console.error(err);
    res.status(503).json({ status: "erro", banco: "indisponível" });
  }
});

app.get("/api/categorias", async (_req, res) => {
  try {
    const { rows } = await pool.query(
      "SELECT id, nome, exige_quantificacao FROM categoria_desafio ORDER BY id"
    );
    res.json(rows);
  } catch (err) {
    console.error(err);
    res.status(500).json({ erro: "Não foi possível carregar as categorias." });
  }
});

app.post("/api/grupos", async (req, res) => {
  const erros = validarGrupo(req.body);
  if (erros.length) return res.status(400).json({ erro: erros.join(" ") });

  const d = req.body;
  let client;
  try {
    client = await pool.connect();
    await client.query("BEGIN");

    const categoriaResult = await client.query(
      "SELECT id, exige_quantificacao FROM categoria_desafio WHERE nome = $1",
      [d.categoria.trim()]
    );
    if (!categoriaResult.rowCount) {
      await client.query("ROLLBACK");
      return res.status(400).json({ erro: "A categoria selecionada não existe." });
    }
    const categoria = categoriaResult.rows[0];
    if (categoria.exige_quantificacao && !(d.formaQuantificacao || "").trim()) {
      await client.query("ROLLBACK");
      return res.status(400).json({ erro: "Informe como o progresso será medido para a categoria Outro." });
    }

    const result = await client.query(
      `SELECT criar_grupo_desafio(
        $1::BIGINT, $2::VARCHAR, $3::TEXT, $4::SMALLINT,
        $5::INTEGER, $6::unidade_duracao, $7::TEXT, $8::VARCHAR,
        $9::visibilidade, $10::INTEGER, $11::TEXT
      ) AS id`,
      [
        demoUserId,
        d.nome.trim(),
        d.atividade.trim(),
        categoria.id,
        Number(d.duracao.quantidade),
        unidades[d.duracao.unidade],
        d.criteriosPontuacao.trim(),
        d.formaQuantificacao ? d.formaQuantificacao.trim() : null,
        visibilidades[d.visibilidade || "publico"],
        d.limiteParticipantes === "" || d.limiteParticipantes == null
          ? null : Number(d.limiteParticipantes),
        d.premiacao ? d.premiacao.trim() : null
      ]
    );

    const grupoId = result.rows[0].id;
    const grupo = await carregarGrupo(client, grupoId);
    await client.query("COMMIT");

    res.status(201).json(formatarGrupo(grupo));
  } catch (err) {
    if (client) {
      try { await client.query("ROLLBACK"); } catch (_) {}
    }
    console.error("Erro ao criar grupo:", err);
    if (err.code === "23503") return res.status(400).json({ erro: "Usuário ou categoria não encontrados. Confira a configuração inicial do banco." });
    if (err.code === "23514") return res.status(400).json({ erro: err.message || "Os dados não atendem às regras do banco." });
    res.status(500).json({ erro: "Não foi possível criar o grupo. Verifique os dados e tente novamente." });
  } finally {
    if (client) client.release();
  }
});

app.put("/api/grupos/:id", async (req, res) => {
  if (!/^[1-9]\d*$/.test(req.params.id)) {
    return res.status(400).json({ erro: "ID de grupo inválido." });
  }

  const erros = validarGrupo(req.body);
  if (erros.length) return res.status(400).json({ erro: erros.join(" ") });

  const d = req.body;
  let client;
  try {
    client = await pool.connect();
    await client.query("BEGIN");

    const categoriaResult = await client.query(
      "SELECT id, exige_quantificacao FROM categoria_desafio WHERE nome = $1",
      [d.categoria.trim()]
    );
    if (!categoriaResult.rowCount) {
      await client.query("ROLLBACK");
      return res.status(400).json({ erro: "A categoria selecionada não existe." });
    }

    const categoria = categoriaResult.rows[0];
    if (categoria.exige_quantificacao && !(d.formaQuantificacao || "").trim()) {
      await client.query("ROLLBACK");
      return res.status(400).json({ erro: "Informe como o progresso será medido para esta categoria." });
    }

    const atualizado = await client.query(
      `UPDATE grupo_desafio
          SET categoria_id = $1,
              nome = $2,
              atividade = $3,
              duracao_quantidade = $4,
              duracao_unidade = $5::unidade_duracao,
              data_fim = (
                data_inicio + CASE $5::unidade_duracao
                  WHEN 'DIAS' THEN make_interval(days => $4)
                  WHEN 'SEMANAS' THEN make_interval(weeks => $4)
                  WHEN 'MESES' THEN make_interval(months => $4)
                END
              )::DATE,
              visibilidade = $6::visibilidade,
              limite_participantes = $7,
              premiacao = NULLIF(btrim($8), ''),
              data_atualizacao = now()
        WHERE id = $9 AND criador_id = $10
        RETURNING id`,
      [
        categoria.id,
        d.nome.trim(),
        d.atividade.trim(),
        Number(d.duracao.quantidade),
        unidades[d.duracao.unidade],
        visibilidades[d.visibilidade || "publico"],
        d.limiteParticipantes === "" || d.limiteParticipantes == null
          ? null : Number(d.limiteParticipantes),
        d.premiacao || "",
        req.params.id,
        demoUserId
      ]
    );

    if (!atualizado.rowCount) {
      await client.query("ROLLBACK");
      return res.status(404).json({ erro: "Grupo não encontrado ou não pertence ao usuário atual." });
    }

    await client.query(
      `UPDATE criterio_pontuacao
          SET descricao = $1,
              unidade_medida = CASE WHEN $2 THEN $3 ELSE NULL END
        WHERE grupo_id = $4`,
      [
        d.criteriosPontuacao.trim(),
        categoria.exige_quantificacao,
        d.formaQuantificacao ? d.formaQuantificacao.trim() : null,
        req.params.id
      ]
    );

    const grupo = await carregarGrupo(client, req.params.id);
    await client.query("COMMIT");
    res.json(formatarGrupo(grupo));
  } catch (err) {
    if (client) {
      try { await client.query("ROLLBACK"); } catch (_) {}
    }
    console.error("Erro ao atualizar grupo:", err);
    if (err.code === "23503") return res.status(400).json({ erro: "Usuário ou categoria não encontrados. Confira a configuração inicial do banco." });
    if (err.code === "23514") return res.status(400).json({ erro: err.message || "Os dados não atendem às regras do banco." });
    res.status(500).json({ erro: "Não foi possível atualizar o grupo. Verifique os dados e tente novamente." });
  } finally {
    if (client) client.release();
  }
});

app.get("*", (_req, res) => {
  res.sendFile(path.join(__dirname, "public", "Who Wins · Criar grupo de desafio (1).html"));
});

app.listen(port, () => {
  console.log(`Who Wins disponível em http://localhost:${port}`);
});

process.on("SIGINT", async () => {
  await pool.end();
  process.exit(0);
});
