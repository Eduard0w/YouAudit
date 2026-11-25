package br.com.youaudit.web;

import java.util.List;

/** Resposta do POST /api/produtos/{id}/extrair: contagens e as afirmações aceitas e descartadas, como estão no banco. */
public record ExtracaoResposta(
        String produtoId,
        Long auditoriaId,
        int versao,
        int blocos,
        int chamadasIa,
        int respostasDoCache,
        int totalAceitas,
        int totalDescartadas,
        List<Aceita> aceitas,
        List<Descartada> descartadas) {

    public record Aceita(int blocoId, String tipo, String trecho, String valor, double confianca) {
    }

    public record Descartada(Integer blocoId, String tipo, String trecho, String motivo, int tentativa) {
    }
}
