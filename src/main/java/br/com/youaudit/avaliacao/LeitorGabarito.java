package br.com.youaudit.avaliacao;

import br.com.youaudit.coleta.Origem;
import br.com.youaudit.extracao.TipoAfirmacao;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Lê o gabarito {@code afirmacoes.csv} (CONTRATOS.md §7: UTF-8, vírgula, campos com aspas podem ter quebra de linha).
 * Só LÊ: o gabarito é do Eduardo (regra 9). Linha que não respeita o formato derruba a leitura com o número da linha,
 * em vez de ser ignorada em silêncio (sumir uma linha mudaria a métrica).
 *
 * <p>Sem biblioteca de CSV (não está na lista de dependências permitidas): {@link #linhas} é um leitor pequeno do
 * formato padrão (RFC 4180). "\r\n" vira "\n", porque o texto dos snapshots usa "\n" e o trecho precisa bater.
 */
@Component
public class LeitorGabarito {

    static final List<String> CABECALHO = List.of("id", "origem", "pagina_id", "tipo", "trecho", "divisao");

    private final Path arquivo;

    public LeitorGabarito(@Value("${avaliacao.gabarito-afirmacoes}") String arquivo) {
        this.arquivo = Path.of(arquivo);
    }

    public boolean existe() {
        return Files.exists(arquivo);
    }

    public List<RotuloAfirmacao> ler() {
        String conteudo;
        try {
            conteudo = Files.readString(arquivo, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("não consegui ler o gabarito " + arquivo, e);
        }
        List<List<String>> linhas = linhas(conteudo);
        if (linhas.isEmpty() || !linhas.get(0).equals(CABECALHO)) {
            throw new IllegalStateException("gabarito: cabeçalho diferente de " + CABECALHO);
        }
        List<RotuloAfirmacao> rotulos = new ArrayList<>();
        for (int i = 1; i < linhas.size(); i++) {
            List<String> c = linhas.get(i);
            if (c.size() != CABECALHO.size()) {
                throw new IllegalStateException("gabarito: registro " + i + " tem " + c.size() + " colunas");
            }
            try {
                rotulos.add(new RotuloAfirmacao(c.get(0), Origem.valueOf(c.get(1)), c.get(2), TipoAfirmacao.valueOf(c.get(3)),
                        c.get(4), c.get(5)));
            } catch (IllegalArgumentException e) {
                throw new IllegalStateException("gabarito: registro " + i + " (" + c.get(0) + ") com origem ou tipo inválido");
            }
        }
        return rotulos;
    }

    /** Quantos registros (sem o cabeçalho) um CSV tem; -1 se o arquivo não existe ou não pôde ser lido. */
    static int contarRegistros(Path arquivo) {
        try {
            return Math.max(0, linhas(Files.readString(arquivo, StandardCharsets.UTF_8)).size() - 1);
        } catch (IOException e) {
            return -1;
        }
    }

    /** Divide o CSV em registros e campos. Aspas duplas dentro de campo com aspas são escritas "". */
    static List<List<String>> linhas(String csv) {
        String texto = csv.replace("\r\n", "\n");
        if (texto.startsWith("﻿")) {
            texto = texto.substring(1); // BOM do Excel
        }
        List<List<String>> registros = new ArrayList<>();
        List<String> atual = new ArrayList<>();
        StringBuilder campo = new StringBuilder();
        boolean entreAspas = false;
        for (int i = 0; i < texto.length(); i++) {
            char ch = texto.charAt(i);
            if (entreAspas) {
                if (ch == '"' && i + 1 < texto.length() && texto.charAt(i + 1) == '"') {
                    campo.append('"');
                    i++;
                } else if (ch == '"') {
                    entreAspas = false;
                } else {
                    campo.append(ch);
                }
            } else if (ch == '"') {
                entreAspas = true;
            } else if (ch == ',') {
                atual.add(campo.toString());
                campo.setLength(0);
            } else if (ch == '\n') {
                atual.add(campo.toString());
                campo.setLength(0);
                registros.add(atual);
                atual = new ArrayList<>();
            } else {
                campo.append(ch);
            }
        }
        if (campo.length() > 0 || !atual.isEmpty()) {
            atual.add(campo.toString());
            registros.add(atual);
        }
        registros.removeIf(r -> r.size() == 1 && r.get(0).isEmpty()); // linhas em branco
        return registros;
    }
}
