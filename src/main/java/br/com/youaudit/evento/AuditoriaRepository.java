package br.com.youaudit.evento;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/** Acesso ao banco da entidade {@link Auditoria}. O Spring gera a implementação a partir da interface. */
public interface AuditoriaRepository extends JpaRepository<Auditoria, Long> {

    /** A auditoria mais recente do produto (maior versão), se existir. */
    Optional<Auditoria> findFirstByProdutoIdOrderByVersaoDesc(String produtoId);

    List<Auditoria> findByProdutoIdOrderByVersaoAsc(String produtoId);

    /**
     * A auditoria mais recente de cada produto, já com o produto carregado ("join fetch"),
     * para a fila não fazer uma consulta extra por linha.
     */
    @Query("""
            select a from Auditoria a join fetch a.produto p
            where a.versao = (select max(a2.versao) from Auditoria a2 where a2.produto = a.produto)
            """)
    List<Auditoria> buscarUltimaDeCadaProduto();
}
