package com.unifor.processardor_consultas.parser;

import com.unifor.processardor_consultas.exception.SqlParseException;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * ETAPA 1 DO PARSING: Tokenização
 *
 * Transforma a string SQL em uma lista de tokens.
 * Cada token representa uma unidade significativa: palavra-chave, identificador,
 * operador, literal, vírgula ou parêntese.
 *
 * Regras aplicadas aqui:
 * - Ignora espaços repetidos (normaliza para espaço único)
 * - Ignora maiúsculas/minúsculas nas palavras-chave
 * - Remove ponto-e-vírgula
 * - Rejeita uso de '*' (SELECT * não é permitido)
 */
public class Tokenizer {

    // Palavras-chave reconhecidas pelo sistema
    private static final Set<String> KEYWORDS = Set.of(
            "SELECT", "FROM", "WHERE", "JOIN", "ON", "AND"
    );

    public List<Token> tokenize(String sql) {
        List<Token> tokens = new ArrayList<>();

        // Pré-processamento: normaliza espaços e remove ';'
        String s = sql.trim().replaceAll("\\s+", " ").replace(";", "");
        int i = 0;
        int n = s.length();

        while (i < n) {
            char c = s.charAt(i);

            // Pula espaços
            if (c == ' ') {
                i++;
                continue;
            }

            // Vírgula — separador de colunas no SELECT
            if (c == ',') {
                tokens.add(new Token(Token.Type.COMMA, ","));
                i++;

            // Parênteses — usados em condições WHERE
            } else if (c == '(') {
                tokens.add(new Token(Token.Type.LPAREN, "("));
                i++;
            } else if (c == ')') {
                tokens.add(new Token(Token.Type.RPAREN, ")"));
                i++;

            // Asterisco — não permitido
            } else if (c == '*') {
                throw new SqlParseException(
                    "Uso de '*' não é permitido. Especifique as colunas (ex: SELECT Cliente.nome).");

            // Operadores de comparação: =, >, <, <=, >=, <>
            } else if (c == '=' || c == '>' || c == '<') {
                if (i == 0 || s.charAt(i - 1) != ' ') {
                    throw new SqlParseException(
                        "Operador '" + c + "' deve ser precedido por um espaço.");
                }
                StringBuilder op = new StringBuilder();
                op.append(c);
                i++;
                // Verifica operadores compostos: <=, >=, <>
                if (i < n) {
                    char next = s.charAt(i);
                    if ((c == '<' && (next == '=' || next == '>')) ||
                            (c == '>' && next == '=')) {
                        op.append(next);
                        i++;
                    }
                }
                if (i < n && s.charAt(i) != ' ') {
                    throw new SqlParseException(
                        "Operador '" + op + "' deve ser seguido por um espaço.");
                }
                tokens.add(new Token(Token.Type.OPERATOR, op.toString()));

            // Palavras: podem ser keywords (SELECT, FROM...) ou identificadores (Cliente.nome)
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

            // Números (literais numéricos)
            } else if (Character.isDigit(c)) {
                StringBuilder num = new StringBuilder();
                while (i < n && (Character.isDigit(s.charAt(i)) || s.charAt(i) == '.')) {
                    num.append(s.charAt(i));
                    i++;
                }
                tokens.add(new Token(Token.Type.LITERAL, num.toString()));

            // Strings entre aspas simples (ex: 'Aberto')
            } else if (c == '\'') {
                StringBuilder str = new StringBuilder("'");
                i++;
                while (i < n && s.charAt(i) != '\'') {
                    str.append(s.charAt(i));
                    i++;
                }
                if (i >= n) throw new SqlParseException("String não fechada — falta aspas simples.");
                str.append('\'');
                i++;
                tokens.add(new Token(Token.Type.LITERAL, str.toString()));

            // Qualquer outro caractere é inválido
            } else {
                throw new SqlParseException("Caractere inesperado: '" + c + "'");
            }
        }
        return tokens;
    }
}
