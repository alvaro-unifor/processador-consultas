package com.unifor.processardor_consultas.dto;

/**
 * Representa uma aresta do grafo de operadores.
 * A direção "from → to" segue o fluxo de resultados intermediários:
 * do operador produtor (filho) para o operador consumidor (pai).
 */
public class EdgeDto {
    private final String from;
    private final String to;

    public EdgeDto(String from, String to) {
        this.from = from;
        this.to = to;
    }

    public String getFrom() { return from; }
    public String getTo() { return to; }
}