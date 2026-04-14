package com.unifor.processardor_consultas.service;

import com.unifor.processardor_consultas.parser.Condition;
import com.unifor.processardor_consultas.parser.JoinClause;
import com.unifor.processardor_consultas.parser.ParsedQuery;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Validates a ParsedQuery against the known schema.
 * Checks table existence and column existence for every reference in the query.
 */
@Service
public class SqlValidatorService {

    private final SchemaService schema;

    public SqlValidatorService(SchemaService schema) {
        this.schema = schema;
    }

    public List<String> validate(ParsedQuery query) {
        List<String> errors = new ArrayList<>();

        // --- Build alias → real table name map ---
        Map<String, String> aliasMap = new HashMap<>();
        registerTable(aliasMap, errors, query.getFromTable(), query.getFromAlias());
        for (JoinClause join : query.getJoins()) {
            registerTable(aliasMap, errors, join.getTable(), join.getAlias());
        }

        // --- Validate SELECT columns ---
        boolean isStar = query.getSelectColumns().size() == 1
                && query.getSelectColumns().get(0).equals("*");
        if (!isStar) {
            for (String col : query.getSelectColumns()) {
                validateColRef(col, aliasMap, errors);
            }
        }

        // --- Validate JOIN ON conditions ---
        for (JoinClause join : query.getJoins()) {
            validateColRef(join.getCondition().getLeft(), aliasMap, errors);
            validateColRefOrLiteral(join.getCondition().getRight(), aliasMap, errors);
        }

        // --- Validate WHERE conditions ---
        for (Condition cond : query.getWhereConditions()) {
            validateColRef(cond.getLeft(), aliasMap, errors);
            validateColRefOrLiteral(cond.getRight(), aliasMap, errors);
        }

        return errors;
    }

    /**
     * Registers a table (and its alias) in the alias map.
     * Also validates that the table exists in the schema.
     */
    private void registerTable(Map<String, String> aliasMap, List<String> errors,
                                String table, String alias) {
        String tableLower = table.toLowerCase();
        if (!schema.tableExists(tableLower)) {
            errors.add("Tabela '" + table + "' não existe no schema");
            return;
        }
        // Map the alias (or the table name itself when there's no alias) → real table
        String key = alias != null ? alias.toLowerCase() : tableLower;
        aliasMap.put(key, tableLower);
        // Always also map the real table name so queries without alias can use table.col
        aliasMap.put(tableLower, tableLower);
    }

    private void validateColRef(String colRef, Map<String, String> aliasMap, List<String> errors) {
        if (colRef.contains(".")) {
            String[] parts = colRef.split("\\.", 2);
            String prefix = parts[0].toLowerCase();
            String col = parts[1].toLowerCase();
            String realTable = aliasMap.get(prefix);
            if (realTable == null) {
                errors.add("Tabela/alias '" + parts[0] + "' não reconhecido");
            } else if (!schema.columnExists(realTable, col)) {
                errors.add("Coluna '" + parts[1] + "' não existe na tabela '" + realTable + "'");
            }
        } else {
            // Unqualified column — check existence in any accessible table
            String col = colRef.toLowerCase();
            boolean found = aliasMap.values().stream()
                    .distinct()
                    .anyMatch(t -> schema.columnExists(t, col));
            if (!found) {
                errors.add("Coluna '" + colRef + "' não encontrada em nenhuma tabela acessível");
            }
        }
    }

    private void validateColRefOrLiteral(String value, Map<String, String> aliasMap,
                                          List<String> errors) {
        if (isLiteral(value)) return;
        validateColRef(value, aliasMap, errors);
    }

    private boolean isLiteral(String value) {
        return value.startsWith("'") || value.matches("-?\\d+(\\.\\d*)?");
    }
}
