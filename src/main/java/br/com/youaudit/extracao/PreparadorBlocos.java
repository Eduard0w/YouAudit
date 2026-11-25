package br.com.youaudit.extracao;

import br.com.youaudit.config.ExtracaoPropriedades;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Passo 3 do pipeline: divide o texto limpo em blocos numerados e agrupa os blocos em chamadas à IA.
 *
 * <p>Cada linha do texto limpo (um parágrafo, ver LimpadorTexto) vira um bloco. Linha maior que
 * {@code maxCaracteresBloco} é cortada no último ". " antes do limite; se não houver, no último espaço;
 * se nem isso, no limite exato. Os blocos são SEMPRE trechos exatos do texto (só os espaços do corte ficam de fora),
 * para a conferência do trecho literal continuar valendo bloco a bloco.
 *
 * <p>É determinístico: o mesmo texto gera sempre os mesmos blocos, então eles não precisam ir para o banco.
 */
@Component
public class PreparadorBlocos {

    private final int maxBloco;
    private final int maxChamada;

    public PreparadorBlocos(ExtracaoPropriedades propriedades) {
        if (propriedades.maxCaracteresBloco() <= 0 || propriedades.maxCaracteresChamada() < propriedades.maxCaracteresBloco()) {
            throw new IllegalArgumentException("extracao.*: o bloco precisa ser > 0 e caber numa chamada");
        }
        this.maxBloco = propriedades.maxCaracteresBloco();
        this.maxChamada = propriedades.maxCaracteresChamada();
    }

    public List<Bloco> dividir(String textoLimpo) {
        List<Bloco> blocos = new ArrayList<>();
        for (String linha : textoLimpo.split("\n")) {
            for (String pedaco : cortar(linha)) {
                if (!pedaco.isEmpty()) {
                    blocos.add(new Bloco(blocos.size() + 1, pedaco));
                }
            }
        }
        return blocos;
    }

    /** Junta blocos seguidos enquanto a soma couber em {@code maxCaracteresChamada}. Nenhum bloco é partido aqui. */
    public List<List<Bloco>> agrupar(List<Bloco> blocos) {
        List<List<Bloco>> grupos = new ArrayList<>();
        List<Bloco> atual = new ArrayList<>();
        int tamanho = 0;
        for (Bloco bloco : blocos) {
            if (!atual.isEmpty() && tamanho + bloco.texto().length() > maxChamada) {
                grupos.add(List.copyOf(atual));
                atual.clear();
                tamanho = 0;
            }
            atual.add(bloco);
            tamanho += bloco.texto().length();
        }
        if (!atual.isEmpty()) {
            grupos.add(List.copyOf(atual));
        }
        return grupos;
    }

    private List<String> cortar(String linha) {
        List<String> pedacos = new ArrayList<>();
        String resto = linha.strip();
        while (resto.length() > maxBloco) {
            int corte = pontoDeCorte(resto);
            pedacos.add(resto.substring(0, corte).strip());
            resto = resto.substring(corte).strip();
        }
        pedacos.add(resto);
        return pedacos;
    }

    /** Onde cortar: depois do último ". " que caiba; senão no último espaço; senão no limite. */
    private int pontoDeCorte(String texto) {
        int ponto = texto.lastIndexOf(". ", maxBloco - 1);
        if (ponto > 0) {
            return ponto + 1; // o ponto fica no bloco de cima
        }
        int espaco = texto.lastIndexOf(' ', maxBloco);
        return espaco > 0 ? espaco : maxBloco;
    }
}
