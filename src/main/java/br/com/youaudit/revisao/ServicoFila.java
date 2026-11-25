package br.com.youaudit.revisao;

import br.com.youaudit.coleta.Produto;
import br.com.youaudit.evento.Auditoria;
import br.com.youaudit.evento.AuditoriaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

/**
 * Monta a fila: a auditoria mais recente de cada produto, da maior pontuação para a menor.
 * Sem pontuação (ainda não calculada, falhou ou coleta insuficiente) vai para o fim; empate: a mais recente primeiro.
 */
@Service
public class ServicoFila {

    private static final Comparator<ItemFila> ORDEM_DA_FILA = Comparator
            .comparing(ItemFila::pontuacao, Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparing(ItemFila::criadaEm, Comparator.reverseOrder());

    private final AuditoriaRepository auditorias;

    public ServicoFila(AuditoriaRepository auditorias) {
        this.auditorias = auditorias;
    }

    @Transactional(readOnly = true)
    public List<ItemFila> listar() {
        return auditorias.buscarUltimaDeCadaProduto().stream()
                .map(ServicoFila::paraItem)
                .sorted(ORDEM_DA_FILA)
                .toList();
    }

    private static ItemFila paraItem(Auditoria a) {
        Produto p = a.getProduto();
        return new ItemFila(p.getId(), p.getNome(), p.getOrigem(), a.getEntrada(), a.getVersao(), a.getStatus(),
                p.getStatus(), a.getPontuacao(), a.getFaixa(), a.getMotivoFalha(), a.getCriadaEm());
    }
}
