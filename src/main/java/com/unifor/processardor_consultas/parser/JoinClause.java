package com.unifor.processardor_consultas.parser;

public class JoinClause {
    private final String table;
    private final String alias;
    private final Condition condition;

    public JoinClause(String table, String alias, Condition condition) {
        this.table = table;
        this.alias = alias;
        this.condition = condition;
    }

    public String getTable() { return table; }
    public String getAlias() { return alias; }
    public Condition getCondition() { return condition; }
}
