package com.unifor.processardor_consultas.dto;

public class NodeDto {
    private final String id;
    private final String type;
    private final String label;

    public NodeDto(String id, String type, String label) {
        this.id = id;
        this.type = type;
        this.label = label;
    }

    public String getId() { return id; }
    public String getType() { return type; }
    public String getLabel() { return label; }
}
