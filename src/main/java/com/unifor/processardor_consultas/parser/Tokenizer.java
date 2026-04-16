package com.unifor.processardor_consultas.parser;

import com.unifor.processardor_consultas.exception.SqlParseException;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class Tokenizer {

    private static final Set<String> KEYWORDS = Set.of(
            "SELECT", "FROM", "WHERE", "JOIN", "ON", "AND", "AS"
    );

    public List<Token> tokenize(String sql) {
        List<Token> tokens = new ArrayList<>();
        String s = sql.trim().replaceAll("\\s+", " ");
        int i = 0;
        int n = s.length();

        while (i < n) {
            char c = s.charAt(i);

            if (c == ' ') {
                i++;
                continue;
            }

            if (c == ',') {
                tokens.add(new Token(Token.Type.COMMA, ","));
                i++;
            } else if (c == '(') {
                tokens.add(new Token(Token.Type.LPAREN, "("));
                i++;
            } else if (c == ')') {
                tokens.add(new Token(Token.Type.RPAREN, ")"));
                i++;
            } else if (c == '*') {
                throw new SqlParseException("Uso de '*' não é permitido. Especifique as colunas explicitamente (ex: SELECT Pedido.idpedido).");
            } else if (c == '=' || c == '>' || c == '<') {
                // Exige espaço antes do operador
                if (i == 0 || s.charAt(i - 1) != ' ') {
                    throw new SqlParseException("Operador '" + c + "' deve ser precedido por um espaço (ex: coluna > valor).");
                }
                StringBuilder op = new StringBuilder();
                op.append(c);
                i++;
                if (i < n) {
                    char next = s.charAt(i);
                    if ((c == '<' && (next == '=' || next == '>')) ||
                            (c == '>' && next == '=')) {
                        op.append(next);
                        i++;
                    }
                }
                // Exige espaço depois do operador
                if (i < n && s.charAt(i) != ' ') {
                    throw new SqlParseException("Operador '" + op + "' deve ser seguido por um espaço (ex: coluna " + op + " valor).");
                }
                tokens.add(new Token(Token.Type.OPERATOR, op.toString()));
            } else if (Character.isLetter(c) || c == '_') {
                StringBuilder word = new StringBuilder();
                while (i < n) {
                    char ch = s.charAt(i);
                    if (Character.isLetterOrDigit(ch) || ch == '_' || ch == '.') {
                        word.append(ch);
                        i++;
                    } else {
                        break;
                    }
                }
                String w = word.toString();
                String upper = w.toUpperCase();
                if (KEYWORDS.contains(upper)) {
                    tokens.add(new Token(Token.Type.valueOf(upper), w));
                } else {
                    tokens.add(new Token(Token.Type.IDENTIFIER, w));
                }
            } else if (Character.isDigit(c)) {
                StringBuilder num = new StringBuilder();
                while (i < n && (Character.isDigit(s.charAt(i)) || s.charAt(i) == '.')) {
                    num.append(s.charAt(i));
                    i++;
                }
                tokens.add(new Token(Token.Type.LITERAL, num.toString()));
            } else if (c == '\'') {
                StringBuilder str = new StringBuilder("'");
                i++;
                while (i < n && s.charAt(i) != '\'') {
                    str.append(s.charAt(i));
                    i++;
                }
                if (i >= n) throw new SqlParseException("String literal not closed");
                str.append('\'');
                i++;
                tokens.add(new Token(Token.Type.LITERAL, str.toString()));
            } else {
                throw new SqlParseException("Caractere inesperado: '" + c + "'");
            }
        }
        return tokens;
    }
}
