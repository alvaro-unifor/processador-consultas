package com.unifor.processardor_consultas.service;

import com.unifor.processardor_consultas.dto.GraphResult;
import com.unifor.processardor_consultas.graph.OperatorNode;
import com.unifor.processardor_consultas.parser.Condition;
import com.unifor.processardor_consultas.parser.JoinClause;
import com.unifor.processardor_consultas.parser.ParsedQuery;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Aplica heurísticas de otimização sobre a árvore de operadores (HU4).
 *
 * Heurísticas implementadas:
 *
 *   H1 – Push de seleções para baixo (σ)
 *        Condições WHERE que referenciam uma única tabela são empurradas para
 *        logo acima do scan dessa tabela, reduzindo tuplas antes dos JOINs.
 *        Condições multi-tabela permanecem acima dos JOINs que as satisfazem.
 *
 *   H2 – Push de projeções intermediárias para baixo (π)
 *        Após cada scan (e possível σ), projeta apenas as colunas daquela
 *        tabela que são realmente usadas nas operações acima (SELECT, JOINs,
 *        WHERE), reduzindo o tamanho das tuplas o mais cedo possível.
 *
 *   H3 – Sem produto cartesiano
 *        Garantido pelo parser: todo JOIN exige cláusula ON.
 */
@Service
public class QueryOptimizerService {

    private final OperatorGraphService graphService;

    public QueryOptimizerService(OperatorGraphService graphService) {
        this.graphService = graphService;
    }

    public GraphResult buildOptimized(ParsedQuery query) {
        return graphService.buildFromTree(buildOptimizedTree(query));
    }

    // -------------------------------------------------------------------------
    // Construção da árvore otimizada
    // -------------------------------------------------------------------------

    private OperatorNode buildOptimizedTree(ParsedQuery query) {
        List<String> allTables = collectAllTables(query);

        // H1: separar WHERE conditions em single-table e multi-table
        Map<String, List<Condition>> tableSelections = new HashMap<>();
        List<Condition> multiTableConditions = new ArrayList<>();

        for (Condition cond : query.getWhereConditions()) {
            Set<String> refs = referencedTables(cond, allTables);
            if (refs.size() == 1) {
                String t = refs.iterator().next();
                tableSelections.computeIfAbsent(t, k -> new ArrayList<>()).add(cond);
            } else {
                multiTableConditions.add(cond);
            }
        }

        // H2: colunas necessárias por tabela
        Map<String, Set<String>> neededCols = collectNeededColumns(query);

        // Nó da tabela do FROM com H1 + H2 aplicados
        OperatorNode current = buildTableNode(query.getFromTable(), tableSelections, neededCols);

        // JOINs encadeados (left-associativo), cada lado direito também otimizado
        for (JoinClause join : query.getJoins()) {
            OperatorNode right = buildTableNode(join.getTable(), tableSelections, neededCols);
            String label = "⋈_{ " + format(join.getCondition()) + " }";
            current = new OperatorNode(OperatorNode.Type.JOIN, label, List.of(current, right));
        }

        // Condições WHERE multi-tabela ficam acima dos JOINs (não podem descer mais)
        if (!multiTableConditions.isEmpty()) {
            String pred = multiTableConditions.stream()
                    .map(this::format)
                    .collect(Collectors.joining(" ∧ "));
            current = new OperatorNode(OperatorNode.Type.SELECTION,
                    "σ_{ " + pred + " }", List.of(current));
        }

        // Raiz: projeção final
        String proj = String.join(", ", query.getSelectColumns());
        return new OperatorNode(OperatorNode.Type.PROJECTION,
                "π_{ " + proj + " }", List.of(current));
    }

    /**
     * Constrói o subárvore de uma única tabela aplicando H1 e H2:
     *   TABLE_SCAN
     *     → σ (condições only desta tabela)    [H1]
     *       → π (somente colunas necessárias)  [H2]
     */
    private OperatorNode buildTableNode(String table,
                                         Map<String, List<Condition>> tableSelections,
                                         Map<String, Set<String>> neededCols) {
        OperatorNode node = new OperatorNode(OperatorNode.Type.TABLE_SCAN, table, List.of());

        // H1: push selection
        List<Condition> conds = tableSelections.get(table.toLowerCase());
        if (conds != null && !conds.isEmpty()) {
            String pred = conds.stream().map(this::format).collect(Collectors.joining(" ∧ "));
            node = new OperatorNode(OperatorNode.Type.SELECTION,
                    "σ_{ " + pred + " }", List.of(node));
        }

        // H2: push intermediate projection
        Set<String> cols = neededCols.get(table.toLowerCase());
        if (cols != null && !cols.isEmpty()) {
            String projLabel = cols.stream()
                    .sorted()
                    .map(c -> table + "." + c)
                    .collect(Collectors.joining(", "));
            node = new OperatorNode(OperatorNode.Type.PROJECTION,
                    "π_{ " + projLabel + " }", List.of(node));
        }

        return node;
    }

    // -------------------------------------------------------------------------
    // Utilitários
    // -------------------------------------------------------------------------

    private List<String> collectAllTables(ParsedQuery query) {
        List<String> tables = new ArrayList<>();
        tables.add(query.getFromTable());
        query.getJoins().forEach(j -> tables.add(j.getTable()));
        return tables;
    }

    /** Retorna os nomes (lowercase) das tabelas referenciadas pelos dois lados da condição. */
    private Set<String> referencedTables(Condition cond, List<String> allTables) {
        Set<String> result = new HashSet<>();
        addTable(cond.getLeft(), allTables, result);
        addTable(cond.getRight(), allTables, result);
        return result;
    }

    private void addTable(String colRef, List<String> allTables, Set<String> result) {
        if (!colRef.contains(".")) return;
        String prefix = colRef.split("\\.")[0].toLowerCase();
        allTables.stream()
                .filter(t -> t.toLowerCase().equals(prefix))
                .findFirst()
                .ifPresent(t -> result.add(t.toLowerCase()));
    }

    /** Coleta, por tabela, quais colunas são usadas em SELECT, JOINs ON e WHERE. */
    private Map<String, Set<String>> collectNeededColumns(ParsedQuery query) {
        Map<String, Set<String>> needed = new HashMap<>();
        query.getSelectColumns().forEach(c -> addCol(c, needed));
        for (JoinClause join : query.getJoins()) {
            addCol(join.getCondition().getLeft(), needed);
            addCol(join.getCondition().getRight(), needed);
        }
        for (Condition cond : query.getWhereConditions()) {
            addCol(cond.getLeft(), needed);
            if (!isLiteral(cond.getRight())) addCol(cond.getRight(), needed);
        }
        return needed;
    }

    private void addCol(String colRef, Map<String, Set<String>> needed) {
        if (!colRef.contains(".")) return;
        String[] parts = colRef.split("\\.", 2);
        needed.computeIfAbsent(parts[0].toLowerCase(), k -> new HashSet<>())
              .add(parts[1].toLowerCase());
    }

    private boolean isLiteral(String v) {
        return v.startsWith("'") || v.matches("-?\\d+(\\.\\d*)?");
    }

    private String format(Condition c) {
        return c.getLeft() + " " + c.getOperator() + " " + c.getRight();
    }
}