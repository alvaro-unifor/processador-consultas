package com.unifor.processardor_consultas.parser;

/**
 * Representa um token extraído da consulta SQL pelo Tokenizer.
 * Cada token tem um tipo (keyword, operador, identificador, etc.) e o valor original.
 */
public class Token {

    public enum Type {
        // Palavras-chave SQL suportadas
        SELECT, FROM, WHERE, JOIN, ON, AND,
        // Elementos estruturais
        IDENTIFIER,  // nomes de tabela/coluna (ex: Cliente.nome)
        OPERATOR,    // operadores de comparação (=, >, <, <=, >=, <>)
        COMMA,       // separador de colunas no SELECT
        LPAREN,      // parêntese de abertura
        RPAREN,      // parêntese de fechamento
        LITERAL      // valores numéricos ou strings entre aspas simples
    }

    private final Type type;
    private final String value;

    public Token(Type type, String value) {
        this.type = type;
        this.value = value;
    }

    public Type getType() { return type; }
    public String getValue() { return value; }

    @Override
    public String toString() { return type + "(" + value + ")"; }
}
