package com.unifor.processardor_consultas.parser;

import java.util.List;

/**
 * Resultado do parsing: contém as partes da consulta SQL já organizadas.
 */
public class ParsedQuery {
    private final List<String> selectColumns;
    private final String fromTable;
    private final List<JoinClause> joins;
    private final List<Condition> whereConditions;

    public ParsedQuery(List<String> selectColumns, String fromTable,
                       List<JoinClause> joins, List<Condition> whereConditions) {
        this.selectColumns = selectColumns;
        this.fromTable = fromTable;
        this.joins = joins;
        this.whereConditions = whereConditions;
    }

    public List<String> getSelectColumns() { return selectColumns; }
    public String getFromTable() { return fromTable; }
    public List<JoinClause> getJoins() { return joins; }
    public List<Condition> getWhereConditions() { return whereConditions; }
}
