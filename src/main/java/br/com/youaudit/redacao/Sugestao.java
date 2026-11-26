package br.com.youaudit.redacao;

/**
 * A correção sugerida para um achado.
 *
 * @param fonte IA (redigida pela IA e aprovada pelo código) ou MODELO (texto de reserva de redacao-modelos.json)
 */
public record Sugestao(String texto, FonteSugestao fonte) {
}
