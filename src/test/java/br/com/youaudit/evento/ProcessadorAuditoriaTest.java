package br.com.youaudit.evento;

import br.com.youaudit.coleta.ConteudoItem;
import br.com.youaudit.coleta.ConteudoItemRepository;
import br.com.youaudit.coleta.Origem;
import br.com.youaudit.coleta.Produto;
import br.com.youaudit.coleta.ProdutoRepository;
import br.com.youaudit.coleta.StatusProduto;
import br.com.youaudit.coleta.TipoConteudo;
import br.com.youaudit.entrega.PromptCruzamento;
import br.com.youaudit.extracao.ClienteIa;
import br.com.youaudit.extracao.FalhaIaException;
import br.com.youaudit.extracao.PedidoIa;
import br.com.youaudit.pontuacao.Faixa;
import br.com.youaudit.redacao.NotificacaoProdutorRepository;
import br.com.youaudit.regras.Achado;
import br.com.youaudit.regras.AchadoRepository;
import br.com.youaudit.regras.Severidade;
import br.com.youaudit.regras.TipoAchado;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * O pipeline inteiro (passos 3 a 9) com o banco e as regras de verdade. Só a IA é trocada: os dois clientes viram
 * dublês que devolvem o que a IA deveria responder para o exemplo "Loja Lucrativa" (CONTEXTO.md §3).
 */
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ProcessadorAuditoriaTest {

    private static final String PAGINA = """
            Método Loja Lucrativa
            Fature R$10 mil por mês em 30 dias
            São 40 aulas práticas
            Bônus: mentoria em grupo
            Acesso por 12 meses
            Garantia de 7 dias
            Apenas R$297 no pix
            Dúvidas? Fale com o suporte pelo e-mail""";

    private static final String EXTRACAO = """
            {"afirmacoes":[
              {"bloco_id":2,"tipo":"RESULTADO_FINANCEIRO","trecho":"Fature R$10 mil por mês","valor":"10000","confianca":0.9},
              {"bloco_id":3,"tipo":"QUANTIDADE_CONTEUDO","trecho":"40 aulas práticas","valor":"40","confianca":0.9},
              {"bloco_id":4,"tipo":"BONUS","trecho":"Bônus: mentoria em grupo","valor":null,"confianca":0.9},
              {"bloco_id":5,"tipo":"ACESSO","trecho":"Acesso por 12 meses","valor":"12","confianca":0.9},
              {"bloco_id":6,"tipo":"GARANTIA","trecho":"Garantia de 7 dias","valor":"7","confianca":0.9},
              {"bloco_id":7,"tipo":"PRECO_CONDICOES","trecho":"Apenas R$297","valor":"297","confianca":0.9}]}""";

    @MockitoBean(name = "clienteIa") private ClienteIa iaPrincipal;
    @MockitoBean(name = "clienteIaSecundario") private ClienteIa iaSecundaria;

    @Autowired private ProcessadorAuditoria processador;
    @Autowired private ProdutoRepository produtos;
    @Autowired private ConteudoItemRepository conteudos;
    @Autowired private AuditoriaRepository auditorias;
    @Autowired private AchadoRepository achados;
    @Autowired private NotificacaoProdutorRepository notificacoes;

    private Long auditoriaId;
    private int itemCusto;

    @BeforeEach
    void preparar() {
        when(iaPrincipal.modelo()).thenReturn("fake-principal");
        when(iaSecundaria.modelo()).thenReturn("fake-secundario");
        Produto p = new Produto("sim-001");
        p.setNome("Método Loja Lucrativa");
        p.setOrigem(Origem.SIMULADO);
        p.setPreco(new BigDecimal("297.00"));
        p.setPaginaTexto(PAGINA);
        p.setStatus(StatusProduto.EM_ANALISE);
        produtos.save(p);
        for (int m = 1; m <= 5; m++) {
            conteudos.save(new ConteudoItem(p, TipoConteudo.MODULO, "Módulo " + m, null, null));
        }
        for (int a = 1; a <= 28; a++) {
            conteudos.save(new ConteudoItem(p, TipoConteudo.AULA, "Aula " + a,
                    a == 12 ? "Requer a ferramenta paga X (assinatura mensal)" : null, 10));
        }
        itemCusto = 5 + 11; // na ordem do banco: 5 módulos, depois as aulas; a aula 12 é o item 16
        auditoriaId = auditorias.save(new Auditoria(p, 1, "a".repeat(64), EntradaAuditoria.EVENTO, Instant.now())).getId();
    }

    @Test
    void lojaLucrativaTerminaCom90AltaAchadosERelatorio() {
        when(iaPrincipal.gerarJson(any())).thenReturn(EXTRACAO);
        when(iaSecundaria.gerarJson(any())).thenAnswer(inv -> {
            PedidoIa pedido = inv.getArgument(0);
            if (pedido.instrucaoSistema().equals(PromptCruzamento.INSTRUCAO_SISTEMA)) {
                return "{\"associacoes\":[{\"promessa\":0,\"item\":null}],\"custos\":[{\"item\":" + itemCusto
                        + ",\"trecho_entrega\":\"Requer a ferramenta paga X\"}]}";
            }
            return "{\"sugestoes\":[]}"; // redação vazia: tudo cai no texto de reserva
        });

        processador.processar(auditoriaId);

        Auditoria a = auditorias.findById(auditoriaId).orElseThrow();
        assertThat(a.getStatus()).isEqualTo(StatusAuditoria.CONCLUIDA);
        assertThat(a.getPontuacao()).isEqualTo(90);
        assertThat(a.getFaixa()).isEqualTo(Faixa.ALTA);
        assertThat(a.getCruzamentoPulado()).isFalse();
        assertThat(produtos.findById("sim-001").orElseThrow().getStatus()).isEqualTo(StatusProduto.AGUARDANDO_DECISAO);

        assertThat(achados.findByAuditoriaId(auditoriaId)).extracting(Achado::getRegraCodigo).containsExactlyInAnyOrder(
                TipoAchado.PROMESSA_RESULTADO_SEM_RESSALVA, TipoAchado.QUANTIDADE_DIVERGENTE,
                TipoAchado.ELEMENTO_AUSENTE_NA_ENTREGA, TipoAchado.CUSTO_OMITIDO);
        Achado custo = achados.findByAuditoriaId(auditoriaId).stream()
                .filter(x -> x.getRegraCodigo() == TipoAchado.CUSTO_OMITIDO).findFirst().orElseThrow();
        assertThat(custo.getEvidenciaEntrega()).isEqualTo("Requer a ferramenta paga X");
        assertThat(custo.getSeveridade()).isEqualTo(Severidade.ALTA);
        assertThat(custo.getSugestaoFonte()).isEqualTo("MODELO");

        assertThat(notificacoes.findByAuditoriaId(auditoriaId)).singleElement()
                .satisfies(n -> assertThat(n.getConteudo()).contains("ENVIO SIMULADO").contains("40 aulas"));
    }

    @Test
    void iaForaDoArNaExtracaoDeixaFalhouComMotivoEOProdutoNaoFicaPreso() {
        when(iaPrincipal.gerarJson(any())).thenThrow(new FalhaIaException("sem cota"));

        processador.processar(auditoriaId);

        Auditoria a = auditorias.findById(auditoriaId).orElseThrow();
        assertThat(a.getStatus()).isEqualTo(StatusAuditoria.FALHOU);
        assertThat(a.getMotivoFalha()).isEqualTo("IA: sem cota");
        assertThat(produtos.findById("sim-001").orElseThrow().getStatus()).isEqualTo(StatusProduto.AGUARDANDO_DECISAO);
    }

    @Test
    void auditoriaQueNaoEstaEmAnaliseNaoERoda() {
        Auditoria a = auditorias.findById(auditoriaId).orElseThrow();
        a.setStatus(StatusAuditoria.CONCLUIDA);
        auditorias.save(a);

        processador.processar(auditoriaId);

        assertThat(achados.findByAuditoriaId(auditoriaId)).isEmpty();
    }
}
