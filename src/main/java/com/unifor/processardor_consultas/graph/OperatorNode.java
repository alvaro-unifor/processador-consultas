package com.unifor.processardor_consultas.graph;

import java.util.List;

/**
 * Nó do grafo de operadores em memória.
 *
 * Tipos possíveis:
 *   TABLE_SCAN  — folha, representa uma tabela base
 *   JOIN        — nó binário, ⋈ com condição ON
 *   SELECTION   — nó unário, σ com predicado WHERE
 *   PROJECTION  — raiz, π com lista de colunas
 */
public class OperatorNode {

    public enum Type {
        TABLE_SCAN,
        JOIN,
        SELECTION,
        PROJECTION
    }

    private final Type type;
    private final String label;
    private final List<OperatorNode> children;

    public OperatorNode(Type type, String label, List<OperatorNode> children) {
        this.type = type;
        this.label = label;
        this.children = children;
    }

    public Type getType() { return type; }
    public String getLabel() { return label; }
    public List<OperatorNode> getChildren() { return children; }
}
