package br.com.youaudit.avaliacao;

/**
 * O contrato comum do baseline e do YouAudit (CONTRATOS.md §7): os dois recebem a MESMA entrada e devolvem o resultado
 * no MESMO formato, para serem comparados contra o mesmo gabarito sem favorecer nenhum dos dois.
 */
public interface Verificador {

    /** Nome que aparece no comparativo, ex.: "baseline" ou "youaudit". */
    String nome();

    ResultadoVerificacao verificar(EntradaVerificacao entrada);
}
