# CLAUDE.md

Processador de Consultas (Histórias de Usuário)
Este documento descreve o trabalho de implementação de um Processador de Consultas, reestruturado no formato
de Histórias de Usuário. O objetivo é alinhar as regras de negócio, critérios de aceitação e métricas de avaliação para
a disciplina de Banco de Dados.

HU1 – Entrada e Validação da Consulta
Como usuário do sistema (aluno/desenvolvedor), quero digitar uma consulta SQL na interface gráfica para que o
sistema valide sintaxe, tabelas e atributos existentes.
Critérios de Aceitação:
• Interface	gráfica	com	campo	de	entrada	da	consulta.

• Parser	deve	validar	comandos	SQL	básicos selecionados	para	o	trabalho (SELECT,	FROM,	WHERE,	JOIN,
ON).

• Operadores	válidos para	o	trabalho (=,	>,	<,	<=,	>=,	<>,	AND,	(	)).

• Verificação	de	existência	de	tabelas	e	atributos.

Regras de Negócio:

• Apenas	tabelas/atributos listados	no	modelo	podem	ser	usados (Imagem	01).

• Consultas	devem	suportar	múltiplos	JOINs (0,	1,…,N).

• Deve	ignorar	a	diferença	entre	palavras	maiúsculas	e	minúsculas.

• Deve	ignorar	repetições	de	espaços	em	branco.

HU2 – Conversão para Álgebra Relacional

Como aluno, quero que minha consulta SQL seja convertida em uma expressão de álgebra relacional, para
compreender a representação teórica.

Critérios de Aceitação:

• Exibir	a	consulta	equivalente	em	álgebra	relacional na	interface	gráfica.

• A	conversão	deve	preservar	operadores	e	condições.

Regras de Negócio:

• Representação	deve	incluir	seleção	(σ),	projeção (π) e junções (⋈)

HU3 – Construção do Grafo de Operadores

Como aluno, quero que o sistema construa um grafo de operadores, para visualizar a estratégia de execução da
consulta.

Critérios de Aceitação:

• O	grafo	deve	ser	gerado	em	memória	e	exibido	na	interface.

• Cada	nó	deve	representar	operadores.

• Arestas	devem	representar	fluxo	de	resultados	intermediários.

• As	folhas	devem	representar	as	tabelas.

• A	raiz	deve	representar	a	última	projeção.

• O	grafo	representa	a	estratégia	de	execução.

Regras de Negócio:

• O	grafo	deve	respeitar	dependências	lógicas	da	consulta.

HU4 – Otimização da Consulta

Como aluno, quero que a álgebra relacional seja otimizada conforme heurísticas, para reduzir o custo de execução.

Critérios de Aceitação:

• Aplicar	heurísticas:
seleções	que	reduzem	tuplas	primeiro;

projeções	que	reduzem	atributos na	sequência;

seleções	e	junções	mais	restritivas	primeiro;

o evitar	produto	cartesiano.

• Exibir	o	grafo	otimizado.

Regras de Negócio:

• A	árvore	deve	ser	reordenada (ou	construída) para	eficiência,	aplicando	heurísticas.

HU5 – Plano de Execução

Como aluno, quero visualizar a ordem de execução da consulta, para compreender como o banco executaria passo a
passo.

Critérios de Aceitação:

• Exibir	ordem	de	execução	(plano de	execução	ordenado).

• Listar	operações	na	ordem	correta.

Regras de Negócio:

• Execução	deve	seguir	ordem	definida	pelo	grafo	otimizado