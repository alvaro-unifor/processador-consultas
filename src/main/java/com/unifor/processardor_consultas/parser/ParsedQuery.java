package com.unifor.processardor_consultas.parser;

import java.util.List;

public class ParsedQuery {
    private final List<String> selectColumns;
    private final String fromTable;
    private final String fromAlias;
    private final List<JoinClause> joins;
    private final List<Condition> whereConditions;

    public ParsedQuery(List<String> selectColumns, String fromTable, String fromAlias,
                       List<JoinClause> joins, List<Condition> whereConditions) {
        this.selectColumns = selectColumns;
        this.fromTable = fromTable;
        this.fromAlias = fromAlias;
        this.joins = joins;
        this.whereConditions = whereConditions;
    }

    public List<String> getSelectColumns() { return selectColumns; }
    public String getFromTable() { return fromTable; }
    public String getFromAlias() { return fromAlias; }
    public List<JoinClause> getJoins() { return joins; }
    public List<Condition> getWhereConditions() { return whereConditions; }
}
