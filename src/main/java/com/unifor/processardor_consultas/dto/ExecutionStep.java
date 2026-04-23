package com.unifor.processardor_consultas.dto;

/**
 * Representa um passo do plano de execução (HU5).
 *
 * Cada passo corresponde a uma operação da árvore otimizada, ordenada
 * de baixo para cima (folhas primeiro, raiz por último).
 */
public class ExecutionStep {
    private final int order;
    private final String operation;
    private final String description;

    public ExecutionStep(int order, String operation, String description) {
        this.order = order;
        this.operation = operation;
        this.description = description;
    }

    public int getOrder() { return order; }
    public String getOperation() { return operation; }
    public String getDescription() { return description; }
}