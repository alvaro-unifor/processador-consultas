package com.unifor.processardor_consultas.dto;

import java.util.List;

public class ProcessingResult {
    private final boolean valid;
    private final List<String> errors;
    private final String relationalAlgebra;
    private final GraphResult operatorGraph;
    private final GraphResult optimizedGraph;

    public ProcessingResult(boolean valid, List<String> errors,
                            String relationalAlgebra,
                            GraphResult operatorGraph,
                            GraphResult optimizedGraph) {
        this.valid = valid;
        this.errors = errors;
        this.relationalAlgebra = relationalAlgebra;
        this.operatorGraph = operatorGraph;
        this.optimizedGraph = optimizedGraph;
    }

    public boolean isValid() { return valid; }
    public List<String> getErrors() { return errors; }
    public String getRelationalAlgebra() { return relationalAlgebra; }
    public GraphResult getOperatorGraph() { return operatorGraph; }
    public GraphResult getOptimizedGraph() { return optimizedGraph; }
}