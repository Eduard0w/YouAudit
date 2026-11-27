package br.com.youaudit.revisao;

import br.com.youaudit.coleta.Origem;
import br.com.youaudit.coleta.Produto;
import br.com.youaudit.coleta.ProdutoRepository;
import br.com.youaudit.evento.Auditoria;
import br.com.youaudit.evento.AuditoriaRepository;
import br.com.youaudit.evento.EntradaAuditoria;
import br.com.youaudit.evento.StatusAuditoria;
import br.com.youaudit.regras.Achado;
import br.com.youaudit.regras.AchadoRepository;
import br.com.youaudit.regras.Severidade;
import br.com.youaudit.regras.TipoAchado;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** A parte do banco da comparação: quais versões, qual decisão vale entre elas. */
@DataJpaTest
@Import({ServicoConsulta.class, ComparadorVersoes.class})
class ServicoConsultaComparacaoTest {

    private static final Instant T0 = Instant.parse("2026-11-26T12:00:00Z");

    @Autowired private ServicoConsulta consulta;
    @Autowired private ProdutoRepository produtos;
    @Autowired private AuditoriaRepository auditorias;
    @Autowired private AchadoRepository achados;
    @Autowired private RegistroDecisaoRepository decisoes;

    @Test
    void pedidoDeAjusteEntreAsVersoesFazOAlertaIdenticoContinuar() {
        Produto p = produto();
        Auditoria v1 = concluida(p, 1, T0, 55);
        Auditoria v2 = concluida(p, 2, T0.plusSeconds(600), 30);
        achado(v1, TipoAchado.TERMO_PROIBIDO, "Dinheiro fácil");
        achado(v1, TipoAchado.QUANTIDADE_DIVERGENTE, "8 módulos completos");
        achado(v2, TipoAchado.TERMO_PROIBIDO, "Dinheiro fácil");
        decisoes.save(new RegistroDecisao(p, DecisaoProduto.PEDIR_AJUSTE, "corrigir termo e quantidade", T0.plusSeconds(60)));

        ComparacaoVersoes c = consulta.comparacao("sim-017");

        assertThat(c.houvePedidoAjuste()).isTrue();
        assertThat(c.decisaoEntreVersoes().motivo()).isEqualTo("corrigir termo e quantidade");
        assertThat(c.versaoAnterior()).isEqualTo(1);
        assertThat(c.pontuacaoAnterior()).isEqualTo(55);
        assertThat(c.pontuacaoAtual()).isEqualTo(30);
        assertThat(c.origem()).isEqualTo(Origem.SIMULADO);
        assertThat(c.contagem().continuam()).isEqualTo(1);
        assertThat(c.contagem().resolvidos()).isEqualTo(1);
    }

    @Test
    void decisaoForaDoIntervaloEntreAsVersoesNaoConta() {
        Produto p = produto();
        Auditoria v1 = concluida(p, 1, T0, 30);
        Auditoria v2 = concluida(p, 2, T0.plusSeconds(600), 30);
        achado(v1, TipoAchado.TERMO_PROIBIDO, "Dinheiro fácil");
        achado(v2, TipoAchado.TERMO_PROIBIDO, "Dinheiro fácil");
        // decidida DEPOIS da v2 chegar: não é um pedido feito à versão 1
        decisoes.save(new RegistroDecisao(p, DecisaoProduto.PEDIR_AJUSTE, "tarde", T0.plusSeconds(900)));

        ComparacaoVersoes c = consulta.comparacao("sim-017");

        assertThat(c.houvePedidoAjuste()).isFalse();
        assertThat(c.decisaoEntreVersoes()).isNull();
        assertThat(c.itens()).extracting(ComparadorVersoes.Item::situacao).containsExactly(SituacaoAlerta.IGUAL_ANTERIOR);
    }

    @Test
    void valeAUltimaDecisaoEntreAsVersoes() {
        Produto p = produto();
        concluida(p, 1, T0, 30);
        concluida(p, 2, T0.plusSeconds(600), 30);
        decisoes.save(new RegistroDecisao(p, DecisaoProduto.PEDIR_AJUSTE, "primeiro", T0.plusSeconds(60)));
        decisoes.save(new RegistroDecisao(p, DecisaoProduto.ESCALAR, "depois", T0.plusSeconds(120)));

        ComparacaoVersoes c = consulta.comparacao("sim-017");

        assertThat(c.decisaoEntreVersoes().decisao()).isEqualTo(DecisaoProduto.ESCALAR);
        assertThat(c.houvePedidoAjuste()).isFalse();
    }

    @Test
    void semVersaoAnteriorOuComVersaoNaoConcluidaDa404() {
        Produto p = produto();
        concluida(p, 1, T0, 30);
        assertThatThrownBy(() -> consulta.comparacao("sim-017")).isInstanceOf(NaoEncontradoException.class);

        auditorias.save(new Auditoria(p, 2, "b".repeat(64), EntradaAuditoria.EVENTO, T0.plusSeconds(600)));
        assertThatThrownBy(() -> consulta.comparacao("sim-017")).isInstanceOf(NaoEncontradoException.class);
    }

    private Produto produto() {
        Produto p = new Produto("sim-017");
        p.setNome("sim-017");
        p.setOrigem(Origem.SIMULADO);
        return produtos.save(p);
    }

    private Auditoria concluida(Produto p, int versao, Instant criadaEm, int pontuacao) {
        Auditoria a = new Auditoria(p, versao, String.valueOf(versao).repeat(64), EntradaAuditoria.EVENTO, criadaEm);
        a.setStatus(StatusAuditoria.CONCLUIDA);
        a.setPontuacao(pontuacao);
        return auditorias.save(a);
    }

    private void achado(Auditoria a, TipoAchado tipo, String trecho) {
        achados.save(new Achado(a, null, tipo, Severidade.MEDIA, trecho, null));
    }
}
