package com.unifor.processardor_consultas.parser;

public class Token {

    public enum Type {
        // Keywords
        SELECT, FROM, WHERE, JOIN, ON, AND, AS,
        // Structural
        IDENTIFIER, OPERATOR, COMMA, LPAREN, RPAREN, LITERAL
    }

    private final Type type;
    private final String value;

    public Token(Type type, String value) {
        this.type = type;
        this.value = value;
    }

    public Type getType() { return type; }
    public String getValue() { return value; }
    public String getValueLower() { return value.toLowerCase(); }

    @Override
    public String toString() { return type + "(" + value + ")"; }
}