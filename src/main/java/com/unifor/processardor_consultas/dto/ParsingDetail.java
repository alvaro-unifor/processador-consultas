package com.unifor.processardor_consultas.dto;

import java.util.List;

/**
 * Detalhes do processo de parsing para visualização na interface.
 * Mostra os tokens gerados e a estrutura parseada.
 */
public class ParsingDetail {

    private final List<TokenInfo> tokens;
    private final ParsedStructure parsedStructure;

    public ParsingDetail(List<TokenInfo> tokens, ParsedStructure parsedStructure) {
        this.tokens = tokens;
        this.parsedStructure = parsedStructure;
    }

    public List<TokenInfo> getTokens() { return tokens; }
    public ParsedStructure getParsedStructure() { return parsedStructure; }

    /** Representa um token individual. */
    public static class TokenInfo {
        private final String type;
        private final String value;

        public TokenInfo(String type, String value) {
            this.type = type;
            this.value = value;
        }

        public String getType() { return type; }
        public String getValue() { return value; }
    }

    /** Estrutura da consulta após o parsing. */
    public static class ParsedStructure {
        private final List<String> selectColumns;
        private final String fromTable;
        private final List<JoinInfo> joins;
        private final List<String> whereConditions;

        public ParsedStructure(List<String> selectColumns, String fromTable,
                               List<JoinInfo> joins, List<String> whereConditions) {
            this.selectColumns = selectColumns;
            this.fromTable = fromTable;
            this.joins = joins;
            this.whereConditions = whereConditions;
        }

        public List<String> getSelectColumns() { return selectColumns; }
        public String getFromTable() { return fromTable; }
        public List<JoinInfo> getJoins() { return joins; }
        public List<String> getWhereConditions() { return whereConditions; }
    }

    /** Representa um JOIN parseado. */
    public static class JoinInfo {
        private final String table;
        private final String condition;

        public JoinInfo(String table, String condition) {
            this.table = table;
            this.condition = condition;
        }

        public String getTable() { return table; }
        public String getCondition() { return condition; }
    }
}
