package br.com.youaudit.evento;

/** O que disparou o evento. No protótipo, todos chegam pelo mesmo endpoint simulado. */
public enum TipoEvento {
    PRODUTO_ENVIADO,
    PRODUTO_REENVIADO,
    PAGINA_ALTERADA
}
