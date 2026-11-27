package br.com.youaudit.revisao;

/**
 * Como um alerta mudou de uma versão do produto para a seguinte (reenvio). Critério definido pelo Eduardo em 26/11.
 *
 * <ul>
 *   <li>{@link #RESOLVIDO}: estava na versão anterior e não aparece na nova.</li>
 *   <li>{@link #CONTINUA}: voltou idêntico DEPOIS de um pedido de ajuste (o produtor não mexeu nesse ponto).</li>
 *   <li>{@link #IGUAL_ANTERIOR}: voltou idêntico, mas sem pedido de ajuste antes (rótulo neutro: nada foi pedido).</li>
 *   <li>{@link #NOVO}: está na nova versão sem um par idêntico na anterior. Uma frase reescrita que ainda é
 *       sinalizada também é NOVO (e a antiga aparece como RESOLVIDO): a pessoa vê as duas e julga.</li>
 * </ul>
 */
public enum SituacaoAlerta {
    RESOLVIDO,
    CONTINUA,
    IGUAL_ANTERIOR,
    NOVO
}
