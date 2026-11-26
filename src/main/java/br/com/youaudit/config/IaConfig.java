package br.com.youaudit.config;

import br.com.youaudit.extracao.ClienteIa;
import br.com.youaudit.extracao.ClienteIaGemini;
import br.com.youaudit.extracao.ClienteIaGroq;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Os dois clientes da IA. {@code ia.provedor} escolhe quem responde: {@code groq} (padrão desde 26/11) ou
 * {@code gemini} (a implementação antiga, mantida).
 *
 * <ul>
 *   <li><b>principal</b>: só a EXTRAÇÃO. É a parte medida contra o gabarito, então fica inteira num modelo só,
 *       para o resultado ser de um modelo e não de uma mistura.</li>
 *   <li><b>secundário</b>: cruzamento com a entrega e redação. Outro modelo, porque cada modelo tem o seu limite
 *       por minuto e por dia.</li>
 * </ul>
 * Quem quer o secundário pede com {@code @Qualifier(IaConfig.SECUNDARIO)}; sem qualificador, vem o principal.
 * Trocar provedor ou modelo muda a chave do cache (o modelo entra nela): as respostas antigas deixam de valer.
 */
@Configuration
public class IaConfig {

    public static final String SECUNDARIO = "clienteIaSecundario";

    private final String provedor;

    public IaConfig(@Value("${ia.provedor}") String provedor) {
        if (!provedor.equals("groq") && !provedor.equals("gemini")) {
            throw new IllegalStateException("ia.provedor deve ser groq ou gemini, não \"" + provedor + "\"");
        }
        this.provedor = provedor;
    }

    @Bean
    @Primary
    public ClienteIa clienteIa(GeminiPropriedades gemini, GroqPropriedades groq) {
        return provedor.equals("groq") ? new ClienteIaGroq(groq, groq.model()) : new ClienteIaGemini(gemini);
    }

    @Bean
    @Qualifier(SECUNDARIO)
    public ClienteIa clienteIaSecundario(GeminiPropriedades gemini, GroqPropriedades groq) {
        return provedor.equals("groq") ? new ClienteIaGroq(groq, groq.modelSecundario())
                : new ClienteIaGemini(gemini, gemini.modelSecundario());
    }
}
