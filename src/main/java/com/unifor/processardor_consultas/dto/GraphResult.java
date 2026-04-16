package com.unifor.processardor_consultas.dto;

import java.util.List;

/**
 * Representação serializada do grafo de operadores (nós + arestas).
 */
public class GraphResult {
    private final List<NodeDto> nodes;
    private final List<EdgeDto> edges;

    public GraphResult(List<NodeDto> nodes, List<EdgeDto> edges) {
        this.nodes = nodes;
        this.edges = edges;
    }

    public List<NodeDto> getNodes() { return nodes; }
    public List<EdgeDto> getEdges() { return edges; }
}