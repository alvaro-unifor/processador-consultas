package com.unifor.processardor_consultas.parser;

import com.unifor.processardor_consultas.exception.SqlParseException;

import java.util.ArrayList;
import java.util.List;

/**
 * Parses a flat token list into a ParsedQuery.
 *
 * Supported grammar:
 *   SELECT col_list FROM table
 *   (JOIN table ON condition)*
 *   (WHERE condition (AND condition)*)?
 *
 * col_list   := col_ref (, col_ref)*
 * condition  := col_ref operator (col_ref | literal)
 * col_ref    := Tabela.coluna  (alias não é permitido)
 * operator   := = | > | < | <= | >= | <>  (espaço obrigatório antes e depois)
 */
public class SqlParser {

    private List<Token> tokens;
    private int pos;

    public ParsedQuery parse(List<Token> tokens) {
        this.tokens = tokens;
        this.pos = 0;

        expect(Token.Type.SELECT);
        List<String> columns = parseSelectList();
        expect(Token.Type.FROM);
        String[] fromRef = parseTableRef();

        List<JoinClause> joins = new ArrayList<>();
        while (check(Token.Type.JOIN)) {
            consume(); // JOIN
            String[] joinRef = parseTableRef();
            expect(Token.Type.ON);
            Condition onCond = parseCondition();
            joins.add(new JoinClause(joinRef[0], joinRef[1], onCond));
        }

        List<Condition> whereConditions = new ArrayList<>();
        if (check(Token.Type.WHERE)) {
            consume(); // WHERE
            // consume optional opening parenthesis
            while (check(Token.Type.LPAREN)) consume();
            whereConditions.add(parseCondition());
            while (check(Token.Type.RPAREN)) consume();

            while (check(Token.Type.AND)) {
                consume(); // AND
                while (check(Token.Type.LPAREN)) consume();
                whereConditions.add(parseCondition());
                while (check(Token.Type.RPAREN)) consume();
            }
        }

        if (pos < tokens.size()) {
            throw new SqlParseException("Token inesperado: '" + tokens.get(pos).getValue() + "'");
        }

        return new ParsedQuery(columns, fromRef[0], fromRef[1], joins, whereConditions);
    }

    private String[] parseTableRef() {
        String table = expectIdentifier("nome de tabela");
        if (check(Token.Type.AS)) {
            throw new SqlParseException("Uso de alias não é permitido. Use o nome completo da tabela (ex: Pedido.idpedido).");
        }
        if (check(Token.Type.IDENTIFIER)) {
            throw new SqlParseException("Alias implícito não é permitido. Use o nome completo da tabela (ex: Pedido.idpedido).");
        }
        return new String[]{table, null};
    }

    private List<String> parseSelectList() {
        List<String> cols = new ArrayList<>();
        cols.add(expectIdentifier("coluna"));
        while (check(Token.Type.COMMA)) {
            consume();
            cols.add(expectIdentifier("coluna"));
        }
        return cols;
    }

    private Condition parseCondition() {
        String left = expectColOrLiteral("lado esquerdo da condição");
        if (!check(Token.Type.OPERATOR)) {
            String found = pos < tokens.size() ? tokens.get(pos).getValue() : "fim da entrada";
            throw new SqlParseException("Operador esperado, encontrado: '" + found + "'");
        }
        String op = consume().getValue();
        String right = expectColOrLiteral("lado direito da condição");
        return new Condition(left, op, right);
    }

    private String expectColOrLiteral(String context) {
        if (check(Token.Type.IDENTIFIER) || check(Token.Type.LITERAL)) {
            return consume().getValue();
        }
        String found = pos < tokens.size() ? tokens.get(pos).getValue() : "fim da entrada";
        throw new SqlParseException("Esperado " + context + ", encontrado: '" + found + "'");
    }

    private String expectIdentifier(String context) {
        if (!check(Token.Type.IDENTIFIER)) {
            String found = pos < tokens.size() ? tokens.get(pos).getValue() : "fim da entrada";
            throw new SqlParseException("Esperado " + context + ", encontrado: '" + found + "'");
        }
        return consume().getValue();
    }

    private void expect(Token.Type type) {
        if (!check(type)) {
            String found = pos < tokens.size() ? tokens.get(pos).getValue() : "fim da entrada";
            throw new SqlParseException("Esperado " + type + ", encontrado: '" + found + "'");
        }
        consume();
    }

    private Token consume() {
        return tokens.get(pos++);
    }

    private boolean check(Token.Type type) {
        return pos < tokens.size() && tokens.get(pos).getType() == type;
    }
}
