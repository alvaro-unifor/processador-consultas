package com.unifor.processardor_consultas.service;

import com.unifor.processardor_consultas.dto.ExecutionStep;
import com.unifor.processardor_consultas.dto.GraphResult;
import com.unifor.processardor_consultas.dto.ParsingDetail;
import com.unifor.processardor_consultas.dto.ProcessingResult;
import com.unifor.processardor_consultas.exception.SqlParseException;
import com.unifor.processardor_consultas.graph.OperatorNode;
import com.unifor.processardor_consultas.parser.*;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

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
            return new ProcessingResult(false, List.of("A consulta não pode ser vazia"),
                    null, null, null, null, null);
        }

        try {
            Tokenizer tokenizer = new Tokenizer();
            SqlParser parser = new SqlParser();

            // Etapa 1: Tokenização
            List<Token> tokens = tokenizer.tokenize(sql);

            // Etapa 2: Análise Sintática
            ParsedQuery parsed = parser.parse(tokens);

            // Monta os detalhes do parsing para visualização
            ParsingDetail parsingDetail = buildParsingDetail(tokens, parsed);

            // Etapa 3: Validação Semântica (schema)
            List<String> errors = validatorService.validate(parsed);
            if (!errors.isEmpty()) {
                return new ProcessingResult(false, errors, parsingDetail,
                        null, null, null, null);
            }

            String ra = raService.convert(parsed);
            GraphResult graph = graphService.build(parsed);

            OperatorNode optimizedTree = optimizerService.buildOptimizedTree(parsed);
            GraphResult optimized = graphService.buildFromTree(optimizedTree);
            List<ExecutionStep> plan = executionPlanService.build(optimizedTree);

            return new ProcessingResult(true, List.of(), parsingDetail,
                    ra, graph, optimized, plan);

        } catch (SqlParseException e) {
            return new ProcessingResult(false, List.of("Erro de sintaxe: " + e.getMessage()),
                    null, null, null, null, null);
        }
    }

    private ParsingDetail buildParsingDetail(List<Token> tokens, ParsedQuery parsed) {
        // Converte tokens para DTOs
        List<ParsingDetail.TokenInfo> tokenInfos = tokens.stream()
                .map(t -> new ParsingDetail.TokenInfo(t.getType().name(), t.getValue()))
                .collect(Collectors.toList());

        // Converte JOINs
        List<ParsingDetail.JoinInfo> joinInfos = parsed.getJoins().stream()
                .map(j -> new ParsingDetail.JoinInfo(
                        j.getTable(),
                        j.getCondition().toString()))
                .collect(Collectors.toList());

        // Converte WHERE conditions
        List<String> whereStrings = parsed.getWhereConditions().stream()
                .map(Condition::toString)
                .collect(Collectors.toList());

        ParsingDetail.ParsedStructure structure = new ParsingDetail.ParsedStructure(
                parsed.getSelectColumns(),
                parsed.getFromTable(),
                joinInfos,
                whereStrings
        );

        return new ParsingDetail(tokenInfos, structure);
    }
}
