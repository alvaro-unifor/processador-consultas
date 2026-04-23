package com.unifor.processardor_consultas.service;

import com.unifor.processardor_consultas.dto.ExecutionStep;
import com.unifor.processardor_consultas.dto.GraphResult;
import com.unifor.processardor_consultas.dto.ProcessingResult;
import com.unifor.processardor_consultas.exception.SqlParseException;
import com.unifor.processardor_consultas.graph.OperatorNode;
import com.unifor.processardor_consultas.parser.ParsedQuery;
import com.unifor.processardor_consultas.parser.SqlParser;
import com.unifor.processardor_consultas.parser.Tokenizer;
import com.unifor.processardor_consultas.parser.Token;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class QueryProcessorService {

    private final SqlValidatorService validatorService;
    private final RelationalAlgebraService raService;
    private final OperatorGraphService graphService;
    private final QueryOptimizerService optimizerService;
    private final ExecutionPlanService executionPlanService;

    public QueryProcessorService(SqlValidatorService validatorService,
                                  RelationalAlgebraService raService,
                                  OperatorGraphService graphService,
                                  QueryOptimizerService optimizerService,
                                  ExecutionPlanService executionPlanService) {
        this.validatorService = validatorService;
        this.raService = raService;
        this.graphService = graphService;
        this.optimizerService = optimizerService;
        this.executionPlanService = executionPlanService;
    }

    public ProcessingResult process(String sql) {
        if (sql == null || sql.isBlank()) {
            return new ProcessingResult(false, List.of("A consulta não pode ser vazia"), null, null, null, null);
        }

        try {
            Tokenizer tokenizer = new Tokenizer();
            SqlParser parser = new SqlParser();

            List<Token> tokens = tokenizer.tokenize(sql);
            ParsedQuery parsed = parser.parse(tokens);

            List<String> errors = validatorService.validate(parsed);
            if (!errors.isEmpty()) {
                return new ProcessingResult(false, errors, null, null, null, null);
            }

            String ra = raService.convert(parsed);
            GraphResult graph = graphService.build(parsed);

            OperatorNode optimizedTree = optimizerService.buildOptimizedTree(parsed);
            GraphResult optimized = graphService.buildFromTree(optimizedTree);
            List<ExecutionStep> plan = executionPlanService.build(optimizedTree);

            return new ProcessingResult(true, List.of(), ra, graph, optimized, plan);

        } catch (SqlParseException e) {
            return new ProcessingResult(false, List.of("Erro de sintaxe: " + e.getMessage()), null, null, null, null);
        }
    }
}
