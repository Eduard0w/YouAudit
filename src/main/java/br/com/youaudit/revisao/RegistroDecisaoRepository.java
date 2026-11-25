package br.com.youaudit.revisao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

/** Acesso ao banco da entidade {@link RegistroDecisao}. O Spring gera a implementação a partir da interface. */
public interface RegistroDecisaoRepository extends JpaRepository<RegistroDecisao, Long> {

    List<RegistroDecisao> findByProdutoIdOrderByDataAsc(String produtoId);
}
