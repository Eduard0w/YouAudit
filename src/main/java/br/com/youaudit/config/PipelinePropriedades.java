package br.com.youaudit.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Linhas "pipeline.*".
 *
 * @param processarRealAutomaticamente false (padrão): auditorias de origem REAL NÃO rodam sozinhas depois do evento;
 *        só pelo POST /api/produtos/{id}/processar. Motivo (PLANO.md): as páginas RESERVADO só podem passar pela IA
 *        UMA vez, depois de congelar prompt e baseline; rodar o lote real automaticamente quebraria isso (e gastaria
 *        a cota diária). Depois do congelamento, pode virar true.
 */
@ConfigurationProperties(prefix = "pipeline")
public record PipelinePropriedades(boolean processarRealAutomaticamente) {
}
