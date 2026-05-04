package com.unifor.processardor_consultas.parser;

import com.unifor.processardor_consultas.exception.SqlParseException;

import java.util.ArrayList;
import java.util.List;

/**
 * ETAPA 2 DO PARSING: Análise Sintática (Parser)
 *
 * Recebe a lista de tokens do Tokenizer e verifica se a estrutura da consulta
 * está correta, produzindo um objeto ParsedQuery com as partes organizadas.
 *
 * Gramática suportada:
 *   SELECT coluna, coluna, ...
 *   FROM tabela
 *   [JOIN tabela ON condição]*     (0 ou mais JOINs)
 *   [WHERE condição [AND condição]*]
 *
 * Onde:
 *   coluna    = Tabela.coluna (formato qualificado)
 *   condição  = valor operador valor
 *   operador  = | > | < | <= | >= | <>
 */
public class SqlParser {

    private List<Token> tokens;
    private int pos;

    public ParsedQuery parse(List<Token> tokens) {
        this.tokens = tokens;
        this.pos = 0;

        // SELECT
        expect(Token.Type.SELECT);
        List<String> columns = parseSelectList();

        // FROM
        expect(Token.Type.FROM);
        String fromTable = expectIdentifier("nome de tabela");

        // JOINs (0 ou mais)
        List<JoinClause> joins = new ArrayList<>();
        while (check(Token.Type.JOIN)) {
            consume(); // consome JOIN
            String joinTable = expectIdentifier("nome de tabela");
            expect(Token.Type.ON);
            Condition onCondition = parseCondition();
            joins.add(new JoinClause(joinTable, onCondition));
        }

        // WHERE (opcional)
        List<Condition> whereConditions = new ArrayList<>();
        if (check(Token.Type.WHERE)) {
            consume(); // consome WHERE
            while (check(Token.Type.LPAREN)) consume();
            whereConditions.add(parseCondition());
            while (check(Token.Type.RPAREN)) consume();

            while (check(Token.Type.AND)) {
                consume(); // consome AND
                while (check(Token.Type.LPAREN)) consume();
                whereConditions.add(parseCondition());
                while (check(Token.Type.RPAREN)) consume();
            }
        }

        // Verifica se todos os tokens foram consumidos
        if (pos < tokens.size()) {
            throw new SqlParseException(
                "Token inesperado: '" + tokens.get(pos).getValue() + "'");
        }

        return new ParsedQuery(columns, fromTable, joins, whereConditions);
    }

    /** Lê a lista de colunas do SELECT, separadas por vírgula. */
    private List<String> parseSelectList() {
        List<String> cols = new ArrayList<>();
        cols.add(expectIdentifier("coluna"));
        while (check(Token.Type.COMMA)) {
            consume();
            cols.add(expectIdentifier("coluna"));
        }
        return cols;
    }

    /** Lê uma condição no formato: valor operador valor */
    private Condition parseCondition() {
        String left = expectValueOrColumn("lado esquerdo da condição");
        if (!check(Token.Type.OPERATOR)) {
            String found = pos < tokens.size() ? tokens.get(pos).getValue() : "fim da consulta";
            throw new SqlParseException("Operador esperado, encontrado: '" + found + "'");
        }
        String op = consume().getValue();
        String right = expectValueOrColumn("lado direito da condição");
        return new Condition(left, op, right);
    }

    /** Espera um identificador (Tabela.coluna) ou literal (número ou string). */
    private String expectValueOrColumn(String context) {
        if (check(Token.Type.IDENTIFIER) || check(Token.Type.LITERAL)) {
            return consume().getValue();
        }
        String found = pos < tokens.size() ? tokens.get(pos).getValue() : "fim da consulta";
        throw new SqlParseException("Esperado " + context + ", encontrado: '" + found + "'");
    }

    /** Espera um identificador (nome de tabela ou coluna). */
    private String expectIdentifier(String context) {
        if (!check(Token.Type.IDENTIFIER)) {
            String found = pos < tokens.size() ? tokens.get(pos).getValue() : "fim da consulta";
            throw new SqlParseException("Esperado " + context + ", encontrado: '" + found + "'");
        }
        return consume().getValue();
    }

    /** Espera um token de tipo específico (ex: SELECT, FROM, ON). */
    private void expect(Token.Type type) {
        if (!check(type)) {
            String found = pos < tokens.size() ? tokens.get(pos).getValue() : "fim da consulta";
            throw new SqlParseException("Esperado " + type + ", encontrado: '" + found + "'");
        }
        consume();
    }

    /** Consome e retorna o token atual. */
    private Token consume() {
        return tokens.get(pos++);
    }

    /** Verifica se o próximo token é do tipo esperado, sem consumir. */
    private boolean check(Token.Type type) {
        return pos < tokens.size() && tokens.get(pos).getType() == type;
    }
}
