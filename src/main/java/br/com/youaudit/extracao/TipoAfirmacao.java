package br.com.youaudit.extracao;

/** Os 14 tipos de afirmação que a IA pode devolver (CONTRATOS.md §1). Qualquer outro valor é rejeitado. */
public enum TipoAfirmacao {
    RESULTADO_FINANCEIRO,
    PRAZO_RESULTADO,
    QUANTIDADE_CONTEUDO,
    CERTIFICADO,
    RECONHECIMENTO_OFICIAL,
    BONUS,
    ACESSO,
    GARANTIA,
    PRECO_CONDICOES,
    CUSTO_ADICIONAL,
    DEPOIMENTO_RESULTADO,
    ESCASSEZ_URGENCIA,
    SAUDE_CURA,
    OUTRO
}
