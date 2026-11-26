package br.com.youaudit.config;

import br.com.youaudit.extracao.ClienteIa;
import br.com.youaudit.extracao.ClienteIaGemini;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Dois clientes da IA, um por modelo (decisão de 26/11: a cota gratuita do Gemini é de 20 chamadas por dia POR
 * MODELO, e uma só não dá para o projeto).
 *
 * <ul>
 *   <li><b>principal</b> ({@code gemini.model}): só a EXTRAÇÃO. É a parte medida contra o gabarito, então fica
 *       inteira num modelo só, para o resultado ser de um modelo e não de uma mistura.</li>
 *   <li><b>secundário</b> ({@code gemini.model-secundario}): cruzamento com a entrega e redação das correções.</li>
 * </ul>
 * Quem quer o secundário pede com {@code @Qualifier(IaConfig.SECUNDARIO)}; sem qualificador, vem o principal.
 */
@Configuration
public class IaConfig {

    public static final String SECUNDARIO = "clienteIaSecundario";

    @Bean
    @Primary
    public ClienteIa clienteIa(GeminiPropriedades propriedades) {
        return new ClienteIaGemini(propriedades);
    }

    @Bean
    @Qualifier(SECUNDARIO)
    public ClienteIa clienteIaSecundario(GeminiPropriedades propriedades) {
        return new ClienteIaGemini(propriedades, propriedades.modelSecundario());
    }
}
