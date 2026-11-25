package br.com.youaudit.coleta;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

/** Acesso ao banco da entidade {@link ConteudoItem}. O Spring gera a implementação a partir da interface. */
public interface ConteudoItemRepository extends JpaRepository<ConteudoItem, Long> {

    List<ConteudoItem> findByProdutoId(String produtoId);

    /** Apaga a entrega antiga quando chega uma nova versão do produto. */
    void deleteByProdutoId(String produtoId);
}
