package br.com.youaudit.extracao;

import br.com.youaudit.evento.CalculadoraHash;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

/**
 * Guarda as respostas da IA na tabela cache_ia, para a demo repetir sem a API no ar e sem gastar cota.
 *
 * <p>Chave = tarefa + versão do prompt + modelo + SHA-256 da entrada exata (instrução de sistema e conteúdo).
 * Mesma entrada → mesma chave → resposta do cache. Mudou o prompt, o modelo ou a página → chave nova → chama a IA.
 *
 * <p>Quem usa só deve chamar {@link #guardar} DEPOIS de validar a resposta: se uma resposta inválida fosse guardada,
 * a nova tentativa leria do cache a mesma resposta ruim.
 */
@Component
public class CacheRespostasIa {

    private final CacheIaRepository repositorio;
    private final CalculadoraHash calculadoraHash;
    private final Clock relogio;

    public CacheRespostasIa(CacheIaRepository repositorio, CalculadoraHash calculadoraHash, Clock relogio) {
        this.repositorio = repositorio;
        this.calculadoraHash = calculadoraHash;
        this.relogio = relogio;
    }

    /** Ex.: "extracao:v1:gemini-3.8-flash:3f2a…" (64 caracteres de hash). O "\u0000" separa as partes sem ambiguidade. */
    public String chave(String tarefa, String versaoPrompt, String modelo, PedidoIa pedido) {
        String entrada = pedido.instrucaoSistema() + "\u0000" + pedido.conteudo();
        return tarefa + ":" + versaoPrompt + ":" + modelo + ":" + calculadoraHash.sha256(entrada);
    }

    @Transactional(readOnly = true)
    public Optional<String> buscar(String chave) {
        return repositorio.findById(chave).map(CacheIa::getRespostaJson);
    }

    /** Se a chave já existe, mantém a primeira resposta (a que já foi usada numa auditoria). */
    @Transactional
    public void guardar(String chave, String respostaJson) {
        if (!repositorio.existsById(chave)) {
            repositorio.save(new CacheIa(chave, respostaJson, Instant.now(relogio)));
        }
    }
}
