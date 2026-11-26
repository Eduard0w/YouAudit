package br.com.youaudit.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuração do Groq (linhas "groq.*"). A chave vem SOMENTE da variável de ambiente GROQ_API_KEY (regra 6):
 * no application.properties fica só a referência ${GROQ_API_KEY:}.
 *
 * <p>Limites medidos em 26/11 pelos cabeçalhos da própria API (conta gratuita): 1.000 pedidos por dia e 8.000 tokens
 * por minuto, POR MODELO. O gargalo é o limite por minuto: por isso há mais novas tentativas que no Gemini e a espera
 * respeita o cabeçalho Retry-After do 429.
 *
 * @param apiKey                  a chave; nunca logar, nunca pôr em mensagem de erro
 * @param model                   modelo da EXTRAÇÃO (a parte medida contra o gabarito)
 * @param modelSecundario         modelo do cruzamento e da redação (outro modelo = outro limite por minuto)
 * @param baseUrl                 endereço da API (os testes apontam para um servidor local)
 * @param timeoutMs               tempo máximo de espera por resposta
 * @param tentativasExtras        novas tentativas em erro de rede, 429 ou 5xx
 * @param esperaEntreTentativasMs espera antes da 1ª nova tentativa (sem Retry-After); dobra a cada uma
 * @param esperaMaximaMs          teto de qualquer espera (inclusive a pedida pelo Retry-After)
 * @param reasoningEffort         "low": o modelo pensa pouco antes de responder (gasta menos tokens do limite)
 * @param maxTokensSaida          teto de tokens da resposta
 */
@ConfigurationProperties(prefix = "groq")
public record GroqPropriedades(
        String apiKey,
        String model,
        String modelSecundario,
        String baseUrl,
        int timeoutMs,
        int tentativasExtras,
        long esperaEntreTentativasMs,
        long esperaMaximaMs,
        String reasoningEffort,
        int maxTokensSaida) {

    /** toString de record mostraria a chave: sobrescrito para ela nunca aparecer em log. */
    @Override
    public String toString() {
        return "GroqPropriedades[model=" + model + ", modelSecundario=" + modelSecundario + ", baseUrl=" + baseUrl
                + ", apiKey=" + (apiKey == null || apiKey.isBlank() ? "(vazia)" : "(definida)") + "]";
    }
}
