package com.unifor.processardor_consultas.service;

import com.unifor.processardor_consultas.dto.ExecutionStep;
import com.unifor.processardor_consultas.graph.OperatorNode;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Gera o plano de execução (HU5) a partir da árvore otimizada.
 *
 * A ordem de execução segue um percurso pós-ordem: folhas (tabelas) primeiro,
 * depois seleções/projeções intermediárias, junções, e por último a projeção
 * final na raiz. Isso reflete como o banco executaria a consulta passo a passo.
 */
@Service
public class ExecutionPlanService {

    public List<ExecutionStep> build(OperatorNode root) {
        List<ExecutionStep> steps = new ArrayList<>();
        traverse(root, steps);
        return steps;
    }

    private void traverse(OperatorNode node, List<ExecutionStep> steps) {
        for (OperatorNode child : node.getChildren()) {
            traverse(child, steps);
        }
        steps.add(toStep(node, steps.size() + 1));
    }

    private ExecutionStep toStep(OperatorNode node, int order) {
        String operation = node.getType().name();
        String description = describe(node);
        return new ExecutionStep(order, operation, description);
    }

    private String describe(OperatorNode node) {
        String label = node.getLabel();
        switch (node.getType()) {
            case TABLE_SCAN:
                return "Ler tabela " + label;
            case SELECTION:
                return "Aplicar seleção (σ) com predicado: " + extractInner(label);
            case PROJECTION:
                return "Projetar colunas (π): " + extractInner(label);
            case JOIN:
                return "Executar junção (⋈) com condição: " + extractInner(label);
            default:
                return label;
        }
    }

    /** Extrai o conteúdo entre chaves em labels como "σ_{ pred }" ou "π_{ cols }". */
    private String extractInner(String label) {
        int start = label.indexOf('{');
        int end = label.lastIndexOf('}');
        if (start < 0 || end < 0 || end <= start) return label;
        return label.substring(start + 1, end).trim();
    }
}
