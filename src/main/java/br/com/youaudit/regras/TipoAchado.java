package br.com.youaudit.regras;

/** Os 7 tipos de achado (CONTRATOS.md §1). O peso de cada um fica na tabela {@code regra}, não aqui. */
public enum TipoAchado {
    TERMO_PROIBIDO,
    PROMESSA_RESULTADO_SEM_RESSALVA,
    RECONHECIMENTO_SEM_COMPROVANTE,
    QUANTIDADE_DIVERGENTE,
    ELEMENTO_AUSENTE_NA_ENTREGA,
    CUSTO_OMITIDO,
    INFO_ESSENCIAL_AUSENTE
}
