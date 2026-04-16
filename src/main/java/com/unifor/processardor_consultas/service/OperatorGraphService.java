package com.unifor.processardor_consultas.service;

import com.unifor.processardor_consultas.dto.EdgeDto;
import com.unifor.processardor_consultas.dto.GraphResult;
import com.unifor.processardor_consultas.dto.NodeDto;
import com.unifor.processardor_consultas.graph.OperatorNode;
import com.unifor.processardor_consultas.parser.Condition;
import com.unifor.processardor_consultas.parser.JoinClause;
import com.unifor.processardor_consultas.parser.ParsedQuery;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Constrói o grafo de operadores (HU3) a partir de uma ParsedQuery válida.
 *
 * Estrutura da árvore (de baixo para cima):
 *   Folhas       → TABLE_SCAN  (uma por tabela referenciada)
 *   Nós internos → JOIN        (⋈, um por cláusula JOIN, left-associativo)
 *   Nó unário    → SELECTION   (σ, presente quando há WHERE)
 *   Raiz         → PROJECTION  (π, sempre presente)
 *
 * As arestas representam o fluxo de resultados intermediários: filho → pai.
 */
@Service
public class OperatorGraphService {

    public GraphResult build(ParsedQuery query) {
        return buildFromTree(buildTree(query));
    }

    /** Serializa qualquer OperatorNode já construído em GraphResult (nós + arestas). */
    public GraphResult buildFromTree(OperatorNode root) {
        List<NodeDto> nodes = new ArrayList<>();
        List<EdgeDto> edges = new ArrayList<>();
        flatten(root, null, new AtomicInteger(0), nodes, edges);
        return new GraphResult(nodes, edges);
    }

    // -------------------------------------------------------------------------
    // Construção da árvore em memória
    // -------------------------------------------------------------------------

    private OperatorNode buildTree(ParsedQuery query) {
        // 1. Folha inicial: tabela do FROM
        OperatorNode current = tableScan(query.getFromTable());

        // 2. JOINs encadeados (left-associativo)
        for (JoinClause join : query.getJoins()) {
            OperatorNode rightTable = tableScan(join.getTable());
            String label = "⋈_{ " + format(join.getCondition()) + " }";
            current = new OperatorNode(OperatorNode.Type.JOIN, label, List.of(current, rightTable));
        }

        // 3. SELECTION (σ) — somente se houver WHERE
        if (!query.getWhereConditions().isEmpty()) {
            String predicate = query.getWhereConditions().stream()
                    .map(this::format)
                    .collect(Collectors.joining(" ∧ "));
            current = new OperatorNode(OperatorNode.Type.SELECTION,
                    "σ_{ " + predicate + " }", List.of(current));
        }

        // 4. Raiz: PROJECTION (π)
        String projection = String.join(", ", query.getSelectColumns());
        return new OperatorNode(OperatorNode.Type.PROJECTION,
                "π_{ " + projection + " }", List.of(current));
    }

    private OperatorNode tableScan(String table) {
        return new OperatorNode(OperatorNode.Type.TABLE_SCAN, table, List.of());
    }

    private String format(Condition c) {
        return c.getLeft() + " " + c.getOperator() + " " + c.getRight();
    }

    // -------------------------------------------------------------------------
    // Serialização para listas planas de nós e arestas
    // -------------------------------------------------------------------------

    /**
     * Percorre a árvore em pré-ordem (DFS), atribui IDs e coleta nós e arestas.
     * Aresta: do nó filho (produtor) para o nó pai (consumidor), representando
     * o fluxo de resultados intermediários.
     */
    private void flatten(OperatorNode node, String parentId, AtomicInteger counter,
                         List<NodeDto> nodes, List<EdgeDto> edges) {
        String id = "n" + counter.getAndIncrement();
        nodes.add(new NodeDto(id, node.getType().name(), node.getLabel()));

        if (parentId != null) {
            edges.add(new EdgeDto(id, parentId));
        }

        for (OperatorNode child : node.getChildren()) {
            flatten(child, id, counter, nodes, edges);
        }
    }
}