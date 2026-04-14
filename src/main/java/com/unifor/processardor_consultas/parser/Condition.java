package com.unifor.processardor_consultas.parser;

public class Condition {
    private final String left;
    private final String operator;
    private final String right;

    public Condition(String left, String operator, String right) {
        this.left = left;
        this.operator = operator;
        this.right = right;
    }

    public String getLeft() { return left; }
    public String getOperator() { return operator; }
    public String getRight() { return right; }

    @Override
    public String toString() { return left + " " + operator + " " + right; }
}