package br.com.youaudit.extracao;

import br.com.youaudit.config.ExtracaoPropriedades;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PreparadorBlocosTest {

    private final PreparadorBlocos preparador = new PreparadorBlocos(new ExtracaoPropriedades(40, 100));

    @Test
    void cadaLinhaViraUmBlocoNumeradoAPartirDe1() {
        List<Bloco> blocos = preparador.dividir("Método Loja Lucrativa\n40 aulas\nApenas R$ 297");

        assertThat(blocos).containsExactly(
                new Bloco(1, "Método Loja Lucrativa"), new Bloco(2, "40 aulas"), new Bloco(3, "Apenas R$ 297"));
    }

    @Test
    void linhaLongaEhCortadaDepoisDoUltimoPontoQueCabe() {
        String linha = "Garantia de 7 dias. Acesso por 12 meses. Certificado incluso no final.";

        List<Bloco> blocos = preparador.dividir(linha);

        assertThat(blocos).extracting(Bloco::texto)
                .containsExactly("Garantia de 7 dias. Acesso por 12 meses.", "Certificado incluso no final.");
    }

    @Test
    void semPontoCortaNoUltimoEspaco() {
        String linha = "palavra ".repeat(8).strip(); // 63 caracteres, sem ponto

        assertThat(preparador.dividir(linha)).allSatisfy(b -> assertThat(b.texto().length()).isLessThanOrEqualTo(40))
                .extracting(Bloco::texto).allSatisfy(t -> assertThat(t).doesNotStartWith(" ").doesNotEndWith(" "));
    }

    @Test
    void palavraGiganteSemEspacoEhCortadaNoLimite() {
        assertThat(preparador.dividir("x".repeat(90))).extracting(b -> b.texto().length()).containsExactly(40, 40, 10);
    }

    @Test
    void todoBlocoEhTrechoExatoDoTexto() {
        String texto = "Fature R$10 mil por mês. Sem experiência. Método validado por centenas de alunos em todo o Brasil.\n"
                + "Bônus: mentoria em grupo\n" + "y".repeat(85);

        for (Bloco bloco : preparador.dividir(texto)) {
            assertThat(texto).contains(bloco.texto());
        }
    }

    @Test
    void cortarNaoPerdeTextoAlemDosEspacosDoCorte() {
        String linha = "Garantia de 7 dias. Acesso por 12 meses. Certificado incluso no final.";

        String juntado = String.join(" ", preparador.dividir(linha).stream().map(Bloco::texto).toList());

        assertThat(juntado).isEqualTo(linha);
    }

    @Test
    void agrupaBlocosSemPassarDoLimiteDaChamada() {
        List<Bloco> blocos = List.of(new Bloco(1, "a".repeat(40)), new Bloco(2, "b".repeat(40)),
                new Bloco(3, "c".repeat(30)), new Bloco(4, "d".repeat(5)));

        List<List<Bloco>> grupos = preparador.agrupar(blocos);

        assertThat(grupos).extracting(g -> g.stream().map(Bloco::id).toList())
                .containsExactly(List.of(1, 2), List.of(3, 4));
    }

    @Test
    void textoVazioNaoGeraBlocoNemGrupo() {
        assertThat(preparador.dividir("")).isEmpty();
        assertThat(preparador.agrupar(List.of())).isEmpty();
    }

    @Test
    void configuracaoIncoerenteEhRecusada() {
        assertThatThrownBy(() -> new PreparadorBlocos(new ExtracaoPropriedades(100, 50)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
