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

• Representação	deve	incluir	seleção	(σ),	projeção	(π)	e	junções	(⋈)