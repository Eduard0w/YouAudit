package br.com.youaudit.evento;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Calcula o SHA-256 do texto limpo. Mesma página → mesmo hash; qualquer caractere diferente → hash diferente.
 * É assim que o sistema sabe se a página mudou sem guardar e comparar o texto inteiro.
 * Recebe o texto JÁ LIMPO (ver {@link br.com.youaudit.coleta.LimpadorTexto}); não limpa nada sozinho.
 */
@Component
public class CalculadoraHash {

    public String sha256(String textoLimpo) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(textoLimpo.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException e) {
            // Todo Java é obrigado a ter SHA-256; se faltar, é problema da instalação, não do dado.
            throw new IllegalStateException("SHA-256 indisponível nesta JVM", e);
        }
    }
}
