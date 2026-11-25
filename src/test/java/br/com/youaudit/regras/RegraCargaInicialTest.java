package br.com.youaudit.regras;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

/** Confere que o data.sql carrega as 7 regras com os pesos do CONTRATOS.md §2. */
@DataJpaTest
class RegraCargaInicialTest {

    @Autowired
    private RegraRepository regras;

    @Test
    void carregaUmaRegraAtivaParaCadaTipoDeAchado() {
        assertThat(regras.findByAtivaTrue()).hasSize(TipoAchado.values().length);
        for (TipoAchado tipo : TipoAchado.values()) {
            assertThat(regras.findByCodigo(tipo)).as("regra %s", tipo).isPresent();
        }
    }

    @Test
    void pesosIniciaisSaoOsDoContrato() {
        assertThat(peso(TipoAchado.TERMO_PROIBIDO)).isEqualTo(30);
        assertThat(peso(TipoAchado.PROMESSA_RESULTADO_SEM_RESSALVA)).isEqualTo(20);
        assertThat(peso(TipoAchado.RECONHECIMENTO_SEM_COMPROVANTE)).isEqualTo(25);
        assertThat(peso(TipoAchado.QUANTIDADE_DIVERGENTE)).isEqualTo(15);
        assertThat(peso(TipoAchado.ELEMENTO_AUSENTE_NA_ENTREGA)).isEqualTo(25);
        assertThat(peso(TipoAchado.CUSTO_OMITIDO)).isEqualTo(30);
        assertThat(peso(TipoAchado.INFO_ESSENCIAL_AUSENTE)).isEqualTo(10);
    }

    private int peso(TipoAchado tipo) {
        return regras.findByCodigo(tipo).orElseThrow().getPeso();
    }
}
