package com.unifor.processardor_consultas.service;

import com.unifor.processardor_consultas.parser.Condition;
import com.unifor.processardor_consultas.parser.JoinClause;
import com.unifor.processardor_consultas.parser.ParsedQuery;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * ETAPA 3: Validação Semântica (Schema)
 *
 * Após o parsing sintático, verifica se as tabelas e colunas
 * referenciadas na consulta existem no schema do banco de dados.
 */
@Service
public class SqlValidatorService {

    private final SchemaService schema;

    public SqlValidatorService(SchemaService schema) {
        this.schema = schema;
    }

    public List<String> validate(ParsedQuery query) {
        List<String> errors = new ArrayList<>();

        // 1. Verifica se todas as tabelas existem no schema
        Set<String> tables = new HashSet<>();
        validateTable(query.getFromTable(), tables, errors);
        for (JoinClause join : query.getJoins()) {
            validateTable(join.getTable(), tables, errors);
        }

        // 2. Verifica colunas do SELECT
        for (String col : query.getSelectColumns()) {
            validateColumn(col, tables, errors);
        }

        // 3. Verifica colunas nas condições JOIN ON
        for (JoinClause join : query.getJoins()) {
            validateColumn(join.getCondition().getLeft(), tables, errors);
            validateColumnOrLiteral(join.getCondition().getRight(), tables, errors);
        }

        // 4. Verifica colunas nas condições WHERE
        for (Condition cond : query.getWhereConditions()) {
            validateColumn(cond.getLeft(), tables, errors);
            validateColumnOrLiteral(cond.getRight(), tables, errors);
        }

        return errors;
    }

    /** Verifica se a tabela existe e a registra no conjunto de tabelas da consulta. */
    private void validateTable(String table, Set<String> tables, List<String> errors) {
        String tableLower = table.toLowerCase();
        if (!schema.tableExists(tableLower)) {
            errors.add("Tabela '" + table + "' não existe no schema.");
        }
        tables.add(tableLower);
    }

    /** Verifica se uma referência Tabela.coluna é válida. */
    private void validateColumn(String colRef, Set<String> tables, List<String> errors) {
        if (colRef.contains(".")) {
            String[] parts = colRef.split("\\.", 2);
            String tableName = parts[0].toLowerCase();
            String columnName = parts[1].toLowerCase();
            if (!tables.contains(tableName)) {
                errors.add("Tabela '" + parts[0] + "' não está no FROM/JOIN da consulta.");
            } else if (!schema.columnExists(tableName, columnName)) {
                errors.add("Coluna '" + parts[1] + "' não existe na tabela '" + tableName + "'.");
            }
        } else {
            // Coluna sem qualificação — procura em todas as tabelas da consulta
            String col = colRef.toLowerCase();
            boolean found = tables.stream().anyMatch(t -> schema.columnExists(t, col));
            if (!found) {
                errors.add("Coluna '" + colRef + "' não encontrada em nenhuma tabela da consulta.");
            }
        }
    }

    /** Se for literal (número ou string), não valida. Senão, valida como coluna. */
    private void validateColumnOrLiteral(String value, Set<String> tables, List<String> errors) {
        if (isLiteral(value)) return;
        validateColumn(value, tables, errors);
    }

    private boolean isLiteral(String value) {
        return value.startsWith("'") || value.matches("-?\\d+(\\.\\d*)?");
    }
}
