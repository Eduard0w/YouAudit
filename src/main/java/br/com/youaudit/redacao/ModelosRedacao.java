package br.com.youaudit.redacao;

import br.com.youaudit.regras.TipoAchado;

import java.util.Map;

/** Os textos de reserva por tipo de achado (lidos de redacao-modelos.json por {@code config.RedacaoConfig}). */
public record ModelosRedacao(Map<TipoAchado, String> porTipo) {

    public ModelosRedacao {
        porTipo = Map.copyOf(porTipo);
    }

    /** O modelo com {trecho} trocado pela evidência (ou pelo detalhe, quando o achado não tem trecho). */
    public String texto(TipoAchado tipo, String evidencia) {
        return porTipo.get(tipo).replace("{trecho}", evidencia == null ? "" : evidencia);
    }
}
