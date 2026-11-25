package br.com.youaudit.evento;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

/** Acesso ao banco da entidade {@link EventoProduto}. O Spring gera a implementação a partir da interface. */
public interface EventoProdutoRepository extends JpaRepository<EventoProduto, Long> {

    List<EventoProduto> findByProdutoIdOrderByIdAsc(String produtoId);
}
