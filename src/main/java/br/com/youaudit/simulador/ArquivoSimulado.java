package br.com.youaudit.simulador;

import br.com.youaudit.evento.EventoProdutoRequest;
import br.com.youaudit.regras.TipoAchado;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * O formato de {@code data/simulado/produtos.json}: os produtos fictícios, cada um com o evento que o simulador
 * dispara e as divergências que foram plantadas nele (o "gabarito" do teste B3, confirmado pelo Eduardo).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ArquivoSimulado(List<ProdutoSimulado> produtos, List<EventoProdutoRequest> reenvios) {

    public ArquivoSimulado {
        produtos = produtos == null ? List.of() : List.copyOf(produtos);
        reenvios = reenvios == null ? List.of() : List.copyOf(reenvios);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ProdutoSimulado(EventoProdutoRequest evento,
                                  @JsonProperty("divergencias_plantadas") List<TipoAchado> divergenciasPlantadas,
                                  String observacao) {
    }
}
