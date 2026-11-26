package br.com.youaudit.redacao;

import br.com.youaudit.config.RelogioConfig;
import br.com.youaudit.evento.CalculadoraHash;
import br.com.youaudit.extracao.CacheRespostasIa;
import br.com.youaudit.extracao.ClienteIaFake;
import br.com.youaudit.regras.TipoAchado;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({CacheRespostasIa.class, CalculadoraHash.class, RelogioConfig.class})
class ServicoRedacaoTest {

    private static final String DADOS = "Fature R$10 mil por mês em 30 dias\nSão 40 aulas";
    private static final List<ServicoRedacao.Pedido> PEDIDOS = List.of(
            new ServicoRedacao.Pedido(TipoAchado.PROMESSA_RESULTADO_SEM_RESSALVA, "promessa sem ressalva", "Fature R$10 mil por mês"),
            new ServicoRedacao.Pedido(TipoAchado.QUANTIDADE_DIVERGENTE, "quantidade divergente", "40 aulas"));

    @Autowired private CacheRespostasIa cache;
    private ClienteIaFake ia;
    private ServicoRedacao servico;

    @BeforeEach
    void preparar() {
        ia = new ClienteIaFake();
        Map<TipoAchado, String> textos = new EnumMap<>(TipoAchado.class);
        for (TipoAchado t : TipoAchado.values()) {
            textos.put(t, "Reserva para {trecho}.");
        }
        servico = new ServicoRedacao(ia, cache, new ConferenteNumeros(), new ModelosRedacao(textos));
    }

    @Test
    void sugestoesAprovadasVemDaIa() {
        ia.responder("""
                {"sugestoes":[{"ponto":0,"texto":"Inclua aviso de que resultados variam."},
                              {"ponto":1,"texto":"Ajuste as 40 aulas para o número entregue."}]}""");

        List<Sugestao> s = servico.redigir(PEDIDOS, DADOS);

        assertThat(s).extracting(Sugestao::fonte).containsExactly(FonteSugestao.IA, FonteSugestao.IA);
    }

    @Test
    void numeroInventadoTemNovaTentativaEDepoisUsaAReserva() {
        ia.responder("""
                {"sugestoes":[{"ponto":0,"texto":"Ofereça garantia de 60 dias."},{"ponto":1,"texto":"Ajuste o número."}]}""")
          .responder("""
                {"sugestoes":[{"ponto":0,"texto":"Prometa 90 dias."}]}""");

        List<Sugestao> s = servico.redigir(PEDIDOS, DADOS);

        assertThat(s.get(0)).isEqualTo(new Sugestao("Reserva para Fature R$10 mil por mês.", FonteSugestao.MODELO));
        assertThat(s.get(1)).isEqualTo(new Sugestao("Ajuste o número.", FonteSugestao.IA)); // aprovada na 1ª
        assertThat(ia.pedidos().get(1).conteudo()).contains("[60]");
    }

    @Test
    void iaForaDoArUsaAReservaENaoDerrubaNada() {
        ia.falhar("sem cota");

        List<Sugestao> s = servico.redigir(PEDIDOS, DADOS);

        assertThat(s).extracting(Sugestao::fonte).containsOnly(FonteSugestao.MODELO);
    }

    @Test
    void semAchadoNaoChamaAIa() {
        assertThat(servico.redigir(List.of(), DADOS)).isEmpty();
        assertThat(ia.pedidos()).isEmpty();
    }
}
