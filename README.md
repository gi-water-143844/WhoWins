# WhoWins
Participantes:  
Giovana Ferreira Waterkemper 143844  
Guilherme dos Santos Lira Lopes 143630  
Jamily Gonçalves de Sales Souza 143048    

## Descrição do Projeto 
<p align="justify">O Who Wins é uma plataforma voltada à criação de hábitos e ao desenvolvimento pessoal por meio de desafios coletivos e competições saudáveis entre amigos e pessoas com interesses em comum. Seu objetivo é incentivar a continuidade das atividades e a melhoria pessoal por meio de metas, acompanhamento do progresso e interação social.</p>  
<p align="justify">Para utilizar a plataforma, o usuário deve criar uma conta ou fazer login, podendo configurar seu perfil como público ou privado. É possível criar grupos públicos ou privados e definir desafios com duração, categoria, limite de participantes, método de pontuação e premiação opcional. O método de pontuação é definido pelo criador do grupo, que estabelece como o desempenho dos participantes será quantificado. Os usuários também podem participar de grupos existentes por meio de convites ou pesquisas com filtros. Dentro dos grupos, os participantes registram suas atividades por meio de fotos, informam o tempo realizado e uma unidade de medida correspondente, que pode ser, por exemplo, horas, quilômetros, páginas lidas ou quantidade de exercícios realizados. Os demais integrantes avaliam as comprovações no mesmo dia, e as avaliações registradas não podem ser alteradas.</p>
<p align="justify">A plataforma também oferece um ranking para acompanhar o desempenho dos participantes, armazenando as publicações como histórico de atividades e permitindo comentários e reações. Os usuários podem seguir outras pessoas, acompanhar suas conquistas e receber notificações sobre mudanças de posição no ranking e lembretes para realizar as atividades diárias, evitando perder sua sequência de dias consecutivos (streak). Assim, o Who Wins combina desafios, acompanhamento de resultados e recursos sociais para estimular a motivação e a criação de hábitos.</p>

## Criação de grupo de desafio

Integração da tela de grupos à API Node.js/Express e ao PostgreSQL. O Supabase funciona como PostgreSQL remoto; esta aplicação conecta usando o driver `pg` no backend.

### Requisitos
- Node.js 18 ou superior
- PostgreSQL 13 ou superior, local ou no Supabase
- npm

### 1. Criar o banco
No PostgreSQL, crie o banco:

```sql
CREATE DATABASE who_wins;
```

Conecte-se ao banco e execute, nesta ordem:
1. `sql/BD_criar_grupo.sql`
2. `sql/seed_demo.sql`

O primeiro arquivo cria as tabelas e a função SQL usadas pela tela. O segundo cria um usuário fictício, pois a tela ainda não contém cadastro ou login. Execute o script de criação uma única vez no banco escolhido.

Exemplo usando terminal:

```bash
createdb -U postgres who_wins
psql -U postgres -d who_wins -f sql/BD_criar_grupo.sql
psql -U postgres -d who_wins -f sql/seed_demo.sql
```

Para o Supabase, selecione o banco `postgres` no painel SQL Editor e execute os arquivos `sql/BD_criar_grupo.sql` e `sql/seed_demo.sql`. Se o comando `createdb` não estiver disponível localmente, crie o banco pelo pgAdmin e execute os arquivos no Query Tool.

### 2. Configurar o backend

Na pasta `WhoWins`:

```bash
npm install
cp .env.example .env
```

O backend lê `WhoWins/.env`, mesmo que o processo seja iniciado a partir de outra pasta. Defina `DATABASE_URL` com a connection string URI do banco:

- Local: `postgresql://postgres:SENHA@localhost:5432/who_wins`
- Supabase: copie a URI do painel **Connect** do projeto. Use a senha do banco e o host/porta de conexão exibidos ali; não use a chave `anon`/publishable. Se a senha tiver caracteres especiais de URL, codifique-os antes de incluí-la.

O arquivo `.env` é local e não deve ser enviado ao Git. A mudança de pasta não transfere esse arquivo automaticamente; ao mover o backend, recrie-o a partir de `.env.example` e confira a `DATABASE_URL`.

Se o usuário demo já existia, confira o ID no banco:

```sql
SELECT id, nome_usuario, email FROM usuario WHERE email = 'demo@whowins.local';
```

Coloque esse ID em `DEMO_USER_ID`.

### 3. Executar

```bash
npm start
```

Abra `http://localhost:3000` no navegador. Não abra o HTML diretamente pelo gerenciador de arquivos: ele deve ser servido pelo Express para que as chamadas `/api/...` funcionem.

Para confirmar o acesso ao banco, abra `http://localhost:3000/api/health`; a resposta deve indicar `banco: "conectado"`. Se a conexão falhar depois de mover os arquivos, confira se `WhoWins/.env` existe e contém uma `DATABASE_URL` válida. O arquivo `.env` é ignorado pelo Git, então não é transferido automaticamente com o backend.

### 4. O que está integrado

- `GET /api/health`: verifica se a API consegue acessar o PostgreSQL.
- `GET /api/categorias`: carrega as categorias cadastradas no banco.
- `POST /api/grupos`: valida os dados e chama a função `criar_grupo_desafio(...)`.
- `PUT /api/grupos/:id`: atualiza o grupo existente e seu critério de pontuação, sem criar um novo. Apenas o usuário de demonstração que o criou pode alterá-lo.
- A função SQL salva o grupo, o critério de pontuação e registra o usuário de demonstração como `DONO`, dentro de uma transação.
- O HTML carrega categorias e exibe o resumo retornado pelo banco após criar ou atualizar um grupo.

### 5. Testar a persistência

Depois de criar um grupo pela tela, execute:

```sql
SELECT g.id, g.nome, g.atividade, c.nome AS categoria,
       g.data_inicio, g.data_fim, g.visibilidade,
       p.papel, u.nome_usuario
FROM grupo_desafio g
JOIN categoria_desafio c ON c.id = g.categoria_id
JOIN participacao p ON p.grupo_id = g.id
JOIN usuario u ON u.id = p.usuario_id
ORDER BY g.id DESC;
```

Para conferir o critério de pontuação:

```sql
SELECT g.id, g.nome, cp.descricao, cp.unidade_medida
FROM grupo_desafio g
JOIN criterio_pontuacao cp ON cp.grupo_id = g.id
ORDER BY g.id DESC;
```

### Observações importantes sobre o escopo

1. **Usuário de demonstração:** como a tela não tem login/cadastro, o backend usa `DEMO_USER_ID`. Isso serve para apresentação local da funcionalidade, não para produção. Em uma versão real, o ID deve vir de uma sessão autenticada, nunca de um valor fixo.
2. **Pontos por unidade:** o formulário não pergunta `pontos_por_unidade`; por isso esse campo permanece `NULL`, como permite o SQL.
3. **Categorias:** a lista deixa de ser fixa no JavaScript e passa a ser carregada da tabela `categoria_desafio`.
4. **Segurança:** não exponha o arquivo `.env`, a senha do banco ou a API demo na internet.
5. **Limite de participantes:** o banco armazena o limite; a validação de vagas no ingresso de novos membros pertence ao caso de uso de entrada em grupo.

### Estrutura

```text
WhoWins/
├── public/
│   └── Who Wins · Criar grupo de desafio (1).html
├── sql/
│   ├── BD_criar_grupo.sql
│   └── seed_demo.sql
├── .env.example
├── package.json
├── server.js
└── README.md
```
