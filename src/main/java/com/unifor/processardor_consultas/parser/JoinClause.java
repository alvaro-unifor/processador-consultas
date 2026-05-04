package com.unifor.processardor_consultas.parser;

/**
 * Representa uma cláusula JOIN: a tabela e a condição ON.
 */
public class JoinClause {
    private final String table;
    private final Condition condition;

    public JoinClause(String table, Condition condition) {
        this.table = table;
        this.condition = condition;
    }

    public String getTable() { return table; }
    public Condition getCondition() { return condition; }
}
