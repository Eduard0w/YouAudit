package br.com.youaudit.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuração do Gemini (linhas "gemini.*"). A chave vem SOMENTE da variável de ambiente GEMINI_API_KEY
 * (regra 6 do CLAUDE.md): no application.properties fica só a referência ${GEMINI_API_KEY:}.
 *
 * @param apiKey                   a chave; nunca logar, nunca pôr em mensagem de erro
 * @param model                    nome do modelo (conferido no AI Studio em 25/11: gemini-2.5-flash)
 * @param baseUrl                  endereço da API (os testes apontam para um servidor local)
 * @param timeoutMs                tempo máximo de espera por resposta
 * @param tentativasExtras         novas tentativas em erro de rede, 429 ou 5xx (contrato: 2)
 * @param esperaEntreTentativasMs  espera antes da 1ª nova tentativa; dobra a cada uma
 * @param modelSecundario          modelo do cruzamento e da redação (26/11: a cota gratuita é de 20 chamadas por dia
 *                                 POR MODELO; a extração fica sozinha no {@code model}, que é o que medimos)
 */
@ConfigurationProperties(prefix = "gemini")
public record GeminiPropriedades(
        String apiKey,
        String model,
        String baseUrl,
        int timeoutMs,
        int tentativasExtras,
        long esperaEntreTentativasMs,
        String modelSecundario) {

    /** toString de record mostraria a chave: sobrescrito para ela nunca aparecer em log. */
    @Override
    public String toString() {
        return "GeminiPropriedades[model=" + model + ", modelSecundario=" + modelSecundario + ", baseUrl=" + baseUrl + ", apiKey="
                + (apiKey == null || apiKey.isBlank() ? "(vazia)" : "(definida)") + "]";
    }
}
