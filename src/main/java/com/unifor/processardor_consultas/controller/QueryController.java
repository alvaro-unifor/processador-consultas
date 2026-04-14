package com.unifor.processardor_consultas.controller;

import com.unifor.processardor_consultas.dto.ProcessingResult;
import com.unifor.processardor_consultas.dto.QueryRequest;
import com.unifor.processardor_consultas.service.QueryProcessorService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/consultas")
public class QueryController {

    private final QueryProcessorService processorService;

    public QueryController(QueryProcessorService processorService) {
        this.processorService = processorService;
    }

    /**
     * Processa uma consulta SQL:
     * - Valida sintaxe, tabelas e atributos (HU1)
     * - Converte para álgebra relacional se válida (HU2)
     *
     * POST /api/consultas
     * Body: { "sql": "SELECT ..." }
     */
    @PostMapping
    public ResponseEntity<ProcessingResult> processar(@RequestBody QueryRequest request) {
        ProcessingResult result = processorService.process(request.getSql());
        return ResponseEntity.ok(result);
    }
}
