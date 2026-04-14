package com.unifor.processardor_consultas.service;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Holds the fixed database schema used for query validation.
 * Tables and columns are stored in lowercase for case-insensitive lookup.
 */
@Service
public class SchemaService {

    private static final Map<String, Set<String>> SCHEMA = new HashMap<>();

    static {
        SCHEMA.put("categoria",         Set.of("idcategoria", "descricao"));
        SCHEMA.put("produto",           Set.of("idproduto", "nome", "descricao", "preco", "quantestoque", "categoria_idcategoria"));
        SCHEMA.put("tipocliente",       Set.of("idtipocliente", "descricao"));
        SCHEMA.put("cliente",           Set.of("idcliente", "nome", "email", "nascimento", "senha", "tipocliente_idtipocliente", "dataregistro"));
        SCHEMA.put("tipoendereco",      Set.of("idtipoendereco", "descricao"));
        SCHEMA.put("endereco",          Set.of("idendereco", "enderecopadrao", "logradouro", "numero", "complemento", "bairro", "cidade", "uf", "cep", "tipoendereco_idtipoendereco", "cliente_idcliente"));
        SCHEMA.put("telefone",          Set.of("numero", "cliente_idcliente"));
        SCHEMA.put("status",            Set.of("idstatus", "descricao"));
        SCHEMA.put("pedido",            Set.of("idpedido", "status_idstatus", "datapedido", "valortotalpedido", "cliente_idcliente"));
        SCHEMA.put("pedido_has_produto", Set.of("idpedidoproduto", "pedido_idpedido", "produto_idproduto", "quantidade", "precounitario"));
    }

    public boolean tableExists(String name) {
        return SCHEMA.containsKey(name.toLowerCase());
    }

    public boolean columnExists(String table, String column) {
        Set<String> cols = SCHEMA.get(table.toLowerCase());
        return cols != null && cols.contains(column.toLowerCase());
    }

    /** Returns all known table names (original lowercase form). */
    public Set<String> allTables() {
        return SCHEMA.keySet();
    }

    /** Returns all columns of a table, or empty set if table not found. */
    public Set<String> columnsOf(String table) {
        return SCHEMA.getOrDefault(table.toLowerCase(), Set.of());
    }
}
