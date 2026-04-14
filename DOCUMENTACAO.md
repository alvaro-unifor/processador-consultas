# Documentação — Processador de Consultas

## Visão Geral

O sistema recebe uma consulta SQL via HTTP, valida a sintaxe e o schema, e converte para álgebra relacional. O fluxo completo passa por quatro camadas em sequência:

```
HTTP POST /api/consultas
        │
        ▼
QueryController
        │
        ▼
QueryProcessorService
        │
        ├── Tokenizer          (texto → tokens)
        ├── SqlParser          (tokens → ParsedQuery)
        ├── SqlValidatorService (valida tabelas e colunas)
        └── RelationalAlgebraService (gera expressão algébrica)
```

---

## 1. Entrada HTTP — `QueryController`

**Arquivo:** `controller/QueryController.java`

Recebe uma requisição `POST /api/consultas` com corpo JSON:
```json
{ "sql": "SELECT p.Nome FROM Produto p WHERE p.Preco > 100" }
```

O método `processar()` simplesmente repassa o campo `sql` para o `QueryProcessorService` e devolve o resultado como JSON. Não há lógica de negócio aqui.

---

## 2. Orquestração — `QueryProcessorService`

**Arquivo:** `service/QueryProcessorService.java`

Coordena todas as etapas. O método `process(String sql)` faz:

1. Verifica se o SQL não é nulo ou vazio.
2. Chama `Tokenizer.tokenize(sql)` → lista de tokens.
3. Chama `SqlParser.parse(tokens)` → objeto `ParsedQuery`.
4. Chama `SqlValidatorService.validate(parsedQuery)` → lista de erros.
5. Se houver erros: retorna `ProcessingResult(valid=false, errors=[...], ra=null)`.
6. Se válido: chama `RelationalAlgebraService.convert(parsedQuery)` e retorna `ProcessingResult(valid=true, errors=[], ra="π_{...}(...)")`.

Qualquer `SqlParseException` lançada pelo tokenizador ou parser é capturada aqui e convertida em um erro de sintaxe no resultado.

---

## 3. Tokenizador — `Tokenizer`

**Arquivo:** `parser/Tokenizer.java`

Converte a string SQL bruta em uma lista de `Token`. Cada `Token` tem um **tipo** e um **valor**.

### Pré-processamento

Antes de tokenizar, normaliza o SQL:
```java
sql.trim().replaceAll("\\s+", " ")
```
Isso elimina espaços extras e tabs — o requisito "ignorar repetições de espaços em branco" é resolvido aqui.

### Lógica de leitura (caractere por caractere)

| Caractere encontrado | O que faz |
|---|---|
| Espaço | Pula |
| `,` | Cria token `COMMA` |
| `(` | Cria token `LPAREN` |
| `)` | Cria token `RPAREN` |
| `*` | Cria token `STAR` |
| `=`, `>`, `<` | Lê operador (pode ser `<=`, `>=`, `<>`) |
| Letra ou `_` | Lê palavra inteira (inclui `.` para `tabela.coluna`) |
| Dígito | Lê número completo (inteiro ou decimal) |
| `'` | Lê string literal até fechar a aspa |
| Qualquer outro | Lança `SqlParseException` ("Caractere inesperado") |

### Classificação de palavras

Após ler uma palavra, verifica se ela pertence ao conjunto de palavras-chave:
```
SELECT, FROM, WHERE, JOIN, ON, AND, AS
```
- Se for palavra-chave → tipo `SELECT`, `FROM`, etc.
- Caso contrário → tipo `IDENTIFIER` (nome de tabela, coluna ou alias)

A comparação é feita em **maiúsculas**, o que resolve o requisito "ignorar diferença entre maiúsculas e minúsculas" para palavras-chave.

### Exemplo de saída

Para `SELECT p.Nome FROM Produto p WHERE p.Preco > 100`:
```
SELECT("SELECT")
IDENTIFIER("p.Nome")
FROM("FROM")
IDENTIFIER("Produto")
IDENTIFIER("p")
WHERE("WHERE")
IDENTIFIER("p.Preco")
OPERATOR(">")
LITERAL("100")
```

---

## 4. Parser — `SqlParser`

**Arquivo:** `parser/SqlParser.java`

Recebe a lista de tokens e constrói um `ParsedQuery` seguindo a gramática:

```
SELECT (colunas | *)
FROM tabela [alias]
(JOIN tabela [alias] ON condição)*
(WHERE condição (AND condição)*)?
```

O parser mantém um **índice de posição** (`pos`) que avança conforme consome tokens.

### Métodos principais

#### `parse(List<Token> tokens)`
Ponto de entrada. Executa o fluxo completo:
1. `expect(SELECT)` — consome o token SELECT (erro se ausente).
2. `parseSelectList()` — lê as colunas do SELECT.
3. `expect(FROM)` — consome o token FROM.
4. `parseTableRef()` — lê tabela + alias opcional.
5. Loop `while(check(JOIN))` — para cada JOIN: lê tabela/alias, consome ON, lê condição.
6. `if(check(WHERE))` — lê condições separadas por AND (com parênteses opcionais).
7. Verifica se sobrou algum token inesperado.

#### `parseSelectList()`
- Se o próximo token é `STAR` → retorna `["*"]`.
- Caso contrário: lê identificadores separados por `COMMA`.

#### `parseTableRef()`
- Lê um `IDENTIFIER` como nome da tabela.
- Verifica o próximo token:
  - Se é `AS` → consome e lê o próximo IDENTIFIER como alias.
  - Se é `IDENTIFIER` (não é palavra-chave) → usa como alias diretamente.
  - Caso contrário → sem alias (null).
- Retorna `[tabela, alias]`.

#### `parseCondition()`
- Lê um `IDENTIFIER` ou `LITERAL` como lado esquerdo.
- Lê um `OPERATOR` (`=`, `>`, `<`, `<=`, `>=`, `<>`).
- Lê um `IDENTIFIER` ou `LITERAL` como lado direito.
- Retorna um objeto `Condition(left, operator, right)`.

#### Métodos auxiliares

- `check(tipo)` — retorna `true` se o token na posição atual é do tipo informado, **sem consumir**.
- `consume()` — retorna o token atual e avança `pos`.
- `expect(tipo)` — consome o token se for do tipo esperado, lança erro caso contrário.

### Exemplo de saída

Para a consulta acima, o `ParsedQuery` resultante seria:
```
selectColumns = ["p.Nome"]
fromTable     = "Produto"
fromAlias     = "p"
joins         = []
whereConditions = [Condition("p.Preco", ">", "100")]
```

---

## 5. Schema — `SchemaService`

**Arquivo:** `service/SchemaService.java`

Contém o modelo de dados fixo do banco (as 10 tabelas do trabalho). É um `Map<String, Set<String>>` onde a chave é o nome da tabela em minúsculas e o valor é o conjunto de colunas, também em minúsculas.

```java
"produto" → {"idproduto", "nome", "descricao", "preco", "quantestoque", "categoria_idcategoria"}
```

### Métodos

- `tableExists(name)` — verifica se a tabela existe (comparação em lowercase).
- `columnExists(table, column)` — verifica se a coluna existe naquela tabela (comparação em lowercase).
- `allTables()` — retorna todos os nomes de tabela.
- `columnsOf(table)` — retorna o conjunto de colunas de uma tabela.

---

## 6. Validador — `SqlValidatorService`

**Arquivo:** `service/SqlValidatorService.java`

Recebe o `ParsedQuery` e valida todos os nomes de tabelas e colunas contra o `SchemaService`. Retorna uma lista de erros (vazia se tudo estiver correto).

### Passo 1 — Construir o mapa de aliases

Antes de validar colunas, precisa saber a qual tabela real cada alias aponta.

Para a query `FROM Produto p JOIN Categoria c ON ...`, o mapa fica:
```
"p"         → "produto"
"produto"   → "produto"   (também registra o nome da tabela sem alias)
"c"         → "categoria"
"categoria" → "categoria"
```

O método `registerTable()` faz isso e já valida se a tabela existe. Se não existir, adiciona um erro e não registra no mapa (para evitar erros em cascata nas colunas).

### Passo 2 — Validar colunas do SELECT

Ignora se for `SELECT *`. Caso contrário, chama `validateColRef()` para cada coluna.

### Passo 3 — Validar condições dos JOINs

Para cada JOIN, valida o lado esquerdo e direito da condição ON. O lado direito pode ser literal (não precisa validar).

### Passo 4 — Validar condições do WHERE

Mesma lógica: valida o lado esquerdo como coluna obrigatoriamente, e o lado direito só se não for literal.

### Método `validateColRef(colRef, aliasMap, errors)`

- **Se tem ponto** (ex: `p.Nome`):
  - Separa em `["p", "Nome"]`.
  - Resolve `"p"` no `aliasMap` → `"produto"`.
  - Verifica `schema.columnExists("produto", "nome")`.
  - Se o alias não existe no mapa → erro "Tabela/alias não reconhecido".
  - Se a coluna não existe → erro "Coluna não existe na tabela".

- **Se não tem ponto** (ex: `Nome`):
  - Busca a coluna em **todas** as tabelas acessíveis da query.
  - Se não encontrar em nenhuma → erro "Coluna não encontrada".

### Método `isLiteral(value)`

Retorna `true` se o valor começa com `'` (string) ou corresponde a um número (`-?\\d+(\\.\\d*)?`). Literais não são validados como colunas.

---

## 7. Álgebra Relacional — `RelationalAlgebraService`

**Arquivo:** `service/RelationalAlgebraService.java`

Converte o `ParsedQuery` em uma expressão de álgebra relacional usando três operadores:

| Símbolo | Nome | Corresponde a |
|---|---|---|
| `π` | Projeção | Colunas do SELECT |
| `σ` | Seleção | Condições do WHERE |
| `⋈` | Theta-join | JOIN ... ON |

### Passo 1 — Montar a expressão base (tabelas + joins)

Começa com a tabela do FROM (com alias se houver):
```
Produto p
```

Para cada JOIN, concatena o operador de junção com a condição:
```
Produto p ⋈_{p.Categoria_idCategoria = c.idCategoria} Categoria c
```

Com múltiplos JOINs, o resultado vai sendo encadeado da esquerda para direita:
```
T1 ⋈_{cond1} T2 ⋈_{cond2} T3
```

### Passo 2 — Envolver com σ (se houver WHERE)

Se existem condições no WHERE, envolve a expressão anterior com σ. Múltiplas condições são separadas pelo símbolo `∧` (E lógico da álgebra relacional):

```
σ_{p.Preco > 100 ∧ p.QuantEstoque >= 5}(Produto p)
```

### Passo 3 — Envolver com π (se não for SELECT *)

Se o SELECT não é `*`, envolve tudo com π listando as colunas:

```
π_{p.Nome, p.Preco}(σ_{p.Preco > 100}(Produto p))
```

Se for `SELECT *`, a expressão interna já é o resultado final (sem π).

---

## Exemplos completos

### Exemplo 1 — Consulta simples
**SQL:**
```sql
SELECT * FROM Cliente
```
**Álgebra Relacional:**
```
Cliente
```

---

### Exemplo 2 — Com projeção e seleção
**SQL:**
```sql
SELECT nome, email FROM Cliente WHERE idCliente = 1
```
**Álgebra Relacional:**
```
π_{nome, email}(σ_{idCliente = 1}(Cliente))
```

---

### Exemplo 3 — Com JOIN
**SQL:**
```sql
SELECT p.Nome, c.Descricao
FROM Produto p
JOIN Categoria c ON p.Categoria_idCategoria = c.idCategoria
```
**Álgebra Relacional:**
```
π_{p.Nome, c.Descricao}(Produto p ⋈_{p.Categoria_idCategoria = c.idCategoria} Categoria c)
```

---

### Exemplo 4 — Com múltiplos JOINs e WHERE
**SQL:**
```sql
SELECT cli.Nome, p.Nome, pp.Quantidade
FROM Pedido ped
JOIN Cliente cli ON ped.Cliente_idCliente = cli.idCliente
JOIN Pedido_has_Produto pp ON pp.Pedido_idPedido = ped.idPedido
JOIN Produto p ON pp.Produto_idProduto = p.idProduto
WHERE ped.ValorTotalPedido > 500
```
**Álgebra Relacional:**
```
π_{cli.Nome, p.Nome, pp.Quantidade}(
  σ_{ped.ValorTotalPedido > 500}(
    Pedido ped
    ⋈_{ped.Cliente_idCliente = cli.idCliente} Cliente cli
    ⋈_{pp.Pedido_idPedido = ped.idPedido} Pedido_has_Produto pp
    ⋈_{pp.Produto_idProduto = p.idProduto} Produto p
  )
)
```

---

### Exemplo 5 — Consulta inválida
**SQL:**
```sql
SELECT p.Valor FROM Produto p
```
**Resposta:**
```json
{
  "valid": false,
  "errors": ["Coluna 'Valor' não existe na tabela 'produto'"],
  "relationalAlgebra": null
}
```

---

## Estrutura de Arquivos

```
src/main/java/com/unifor/processardor_consultas/
├── controller/
│   └── QueryController.java          — endpoint POST /api/consultas
├── dto/
│   ├── QueryRequest.java             — { "sql": "..." }
│   └── ProcessingResult.java         — { valid, errors, relationalAlgebra }
├── parser/
│   ├── Token.java                    — tipo + valor de um token
│   ├── Tokenizer.java                — string → List<Token>
│   ├── SqlParser.java                — List<Token> → ParsedQuery
│   ├── ParsedQuery.java              — representação estruturada da query
│   ├── JoinClause.java               — tabela + alias + condição de um JOIN
│   └── Condition.java                — left + operator + right
├── service/
│   ├── SchemaService.java            — schema fixo das 10 tabelas
│   ├── SqlValidatorService.java      — valida tabelas e colunas (HU1)
│   ├── RelationalAlgebraService.java — converte para álgebra relacional (HU2)
│   └── QueryProcessorService.java    — orquestra tokenizer → parser → validador → RA
└── exception/
    └── SqlParseException.java        — erro de sintaxe durante parsing
```
