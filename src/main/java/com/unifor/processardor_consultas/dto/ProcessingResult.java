package com.unifor.processardor_consultas.dto;

import java.util.List;

public class ProcessingResult {
    private final boolean valid;
    private final List<String> errors;
    private final String relationalAlgebra;

    public ProcessingResult(boolean valid, List<String> errors, String relationalAlgebra) {
        this.valid = valid;
        this.errors = errors;
        this.relationalAlgebra = relationalAlgebra;
    }

    public boolean isValid() { return valid; }
    public List<String> getErrors() { return errors; }
    public String getRelationalAlgebra() { return relationalAlgebra; }
}
