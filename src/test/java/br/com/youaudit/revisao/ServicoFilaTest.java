package br.com.youaudit.revisao;

import br.com.youaudit.coleta.Origem;
import br.com.youaudit.coleta.Produto;
import br.com.youaudit.coleta.ProdutoRepository;
import br.com.youaudit.evento.Auditoria;
import br.com.youaudit.evento.AuditoriaRepository;
import br.com.youaudit.evento.EntradaAuditoria;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(ServicoFila.class)
class ServicoFilaTest {

    private static final Instant T0 = Instant.parse("2026-11-25T12:00:00Z");

    @Autowired private ServicoFila fila;
    @Autowired private ProdutoRepository produtos;
    @Autowired private AuditoriaRepository auditorias;

    @Test
    void mostraSoAUltimaVersaoDeCadaProdutoComAEtiquetaDeEntrada() {
        Produto p = produto("pagina-01", Origem.REAL);
        auditorias.save(new Auditoria(p, 1, "a".repeat(64), EntradaAuditoria.EVENTO, T0));
        auditorias.save(new Auditoria(p, 2, "b".repeat(64), EntradaAuditoria.LINK, T0.plusSeconds(60)));

        List<ItemFila> itens = fila.listar();

        assertThat(itens).hasSize(1);
        assertThat(itens.get(0).versao()).isEqualTo(2);
        assertThat(itens.get(0).entrada()).isEqualTo(EntradaAuditoria.LINK);
        assertThat(itens.get(0).origem()).isEqualTo(Origem.REAL);
    }

    @Test
    void ordenaPorPontuacaoESemPontuacaoVaiProFim() {
        Auditoria baixa = auditorias.save(new Auditoria(produto("sim-001", Origem.SIMULADO), 1, null, EntradaAuditoria.EVENTO, T0));
        baixa.setPontuacao(20);
        Auditoria alta = auditorias.save(new Auditoria(produto("sim-002", Origem.SIMULADO), 1, null, EntradaAuditoria.EVENTO, T0));
        alta.setPontuacao(90);
        auditorias.save(new Auditoria(produto("sim-003", Origem.SIMULADO), 1, null, EntradaAuditoria.EVENTO, T0.plusSeconds(5)));

        assertThat(fila.listar()).extracting(ItemFila::produtoId).containsExactly("sim-002", "sim-001", "sim-003");
    }

    private Produto produto(String id, Origem origem) {
        Produto p = new Produto(id);
        p.setNome(id);
        p.setOrigem(origem);
        return produtos.save(p);
    }
}
