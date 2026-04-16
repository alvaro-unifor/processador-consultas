package com.unifor.processardor_consultas.service;

import com.unifor.processardor_consultas.parser.Condition;
import com.unifor.processardor_consultas.parser.JoinClause;
import com.unifor.processardor_consultas.parser.ParsedQuery;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Converts a valid ParsedQuery into a relational algebra expression.
 *
 * Symbols used:
 *   π  — projection  (SELECT columns)
 *   σ  — selection   (WHERE conditions)
 *   ⋈  — theta-join  (JOIN ... ON ...)
 *   ∧  — logical AND (multiple WHERE conditions)
 */
@Service
public class RelationalAlgebraService {

    public String convert(ParsedQuery query) {

        // 1. Build the "from" expression: base table with zero or more joins
        String fromExpr = tableDisplay(query.getFromTable(), query.getFromAlias());
        for (JoinClause join : query.getJoins()) {
            fromExpr = fromExpr
                    + " ⋈_{"
                    + formatCondition(join.getCondition())
                    + "} "
                    + tableDisplay(join.getTable(), join.getAlias());
        }

        // 2. Wrap with σ if there are WHERE conditions
        String inner = fromExpr;
        if (!query.getWhereConditions().isEmpty()) {
            String predicates = query.getWhereConditions().stream()
                    .map(this::formatCondition)
                    .collect(Collectors.joining(" ∧ "));
            inner = "σ_{" + predicates + "}(" + fromExpr + ")";
        }

        // 3. Wrap with π (projeção obrigatória — * não é permitido)
        String projection = String.join(", ", query.getSelectColumns());
        return "π_{" + projection + "}(" + inner + ")";
    }

    private String tableDisplay(String table, String alias) {
        return alias != null ? table + " " + alias : table;
    }

    private String formatCondition(Condition c) {
        return c.getLeft() + " " + c.getOperator() + " " + c.getRight();
    }
}
