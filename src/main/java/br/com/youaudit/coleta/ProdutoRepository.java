package br.com.youaudit.coleta;

import org.springframework.data.jpa.repository.JpaRepository;

/** Acesso ao banco da entidade {@link Produto}. O Spring gera a implementação a partir da interface. */
public interface ProdutoRepository extends JpaRepository<Produto, String> {
}
