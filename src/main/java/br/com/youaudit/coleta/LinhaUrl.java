package br.com.youaudit.coleta;

/** Uma linha válida do data/real/urls.txt: número da linha, id anônimo ("pagina-01") e URL. */
public record LinhaUrl(int numero, String produtoId, String url) {
}
