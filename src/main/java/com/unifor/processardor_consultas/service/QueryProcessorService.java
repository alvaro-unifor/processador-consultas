package com.unifor.processardor_consultas.service;

import com.unifor.processardor_consultas.dto.ProcessingResult;
import com.unifor.processardor_consultas.exception.SqlParseException;
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

    public QueryProcessorService(SqlValidatorService validatorService,
                                  RelationalAlgebraService raService) {
        this.validatorService = validatorService;
        this.raService = raService;
    }

    public ProcessingResult process(String sql) {
        if (sql == null || sql.isBlank()) {
            return new ProcessingResult(false, List.of("A consulta não pode ser vazia"), null);
        }

        try {
            Tokenizer tokenizer = new Tokenizer();
            SqlParser parser = new SqlParser();

            List<Token> tokens = tokenizer.tokenize(sql);
            ParsedQuery parsed = parser.parse(tokens);

            List<String> errors = validatorService.validate(parsed);
            if (!errors.isEmpty()) {
                return new ProcessingResult(false, errors, null);
            }

            String ra = raService.convert(parsed);
            return new ProcessingResult(true, List.of(), ra);

        } catch (SqlParseException e) {
            return new ProcessingResult(false, List.of("Erro de sintaxe: " + e.getMessage()), null);
        }
    }
}
