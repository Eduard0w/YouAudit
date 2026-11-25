package br.com.youaudit.web;

import br.com.youaudit.evento.EventoProdutoRequest;
import br.com.youaudit.evento.EventoResposta;
import br.com.youaudit.evento.ResultadoEvento;
import br.com.youaudit.evento.ServicoEvento;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Testa só a camada web: o serviço é substituído por um dublê do Mockito. */
@WebMvcTest(EventoController.class)
class EventoControllerTest {

    private static final String CORPO_VALIDO = """
            {
              "tipo": "PRODUTO_ENVIADO",
              "produto": {
                "id": "sim-001",
                "nome": "Método Loja Lucrativa",
                "categoria": "curso",
                "preco": 297.00,
                "origem": "SIMULADO",
                "pagina_texto": "40 aulas. Apenas R$ 297.",
                "conteudo": {
                  "itens": [
                    {"tipo": "AULA", "titulo": "Aula 12", "descricao": "Requer assinatura de ferramenta X", "duracao_min": 14}
                  ]
                }
              }
            }
            """;

    @Autowired private MockMvc mvc;
    @MockitoBean private ServicoEvento servico;

    @Test
    void corpoValidoResponde202ComOResultado() throws Exception {
        when(servico.receber(any())).thenReturn(new EventoResposta(1L, ResultadoEvento.PROCESSADO, 10L, 1));

        mvc.perform(post("/api/eventos/produto-enviado").contentType(MediaType.APPLICATION_JSON).content(CORPO_VALIDO))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.resultado").value("PROCESSADO"))
                .andExpect(jsonPath("$.auditoriaId").value(10));
    }

    @Test
    void semPaginaTextoResponde400ENaoChamaOServico() throws Exception {
        String semPagina = CORPO_VALIDO.replace("\"pagina_texto\": \"40 aulas. Apenas R$ 297.\",", "");

        mvc.perform(post("/api/eventos/produto-enviado").contentType(MediaType.APPLICATION_JSON).content(semPagina))
                .andExpect(status().isBadRequest());
        verify(servico, never()).receber(any(EventoProdutoRequest.class));
    }

    @Test
    void tipoDeEventoForaDaEnumeracaoResponde400() throws Exception {
        String tipoInvalido = CORPO_VALIDO.replace("PRODUTO_ENVIADO", "APROVAR_TUDO");

        mvc.perform(post("/api/eventos/produto-enviado").contentType(MediaType.APPLICATION_JSON).content(tipoInvalido))
                .andExpect(status().isBadRequest());
    }

    @Test
    void itemDeConteudoSemTipoResponde400() throws Exception {
        String itemSemTipo = CORPO_VALIDO.replace("\"tipo\": \"AULA\", ", "");

        mvc.perform(post("/api/eventos/produto-enviado").contentType(MediaType.APPLICATION_JSON).content(itemSemTipo))
                .andExpect(status().isBadRequest());
    }
}
