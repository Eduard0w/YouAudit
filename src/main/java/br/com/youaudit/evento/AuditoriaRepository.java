package br.com.youaudit.evento;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

/** Acesso ao banco da entidade {@link Auditoria}. O Spring gera a implementação a partir da interface. */
public interface AuditoriaRepository extends JpaRepository<Auditoria, Long> {

    /** A auditoria mais recente do produto (maior versão), se existir. */
    Optional<Auditoria> findFirstByProdutoIdOrderByVersaoDesc(String produtoId);

    List<Auditoria> findByProdutoIdOrderByVersaoAsc(String produtoId);
}
