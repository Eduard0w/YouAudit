package br.com.youaudit.revisao;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Acesso ao banco da entidade {@link RegistroPromessas}. O Spring gera a implementação a partir da interface. */
public interface RegistroPromessasRepository extends JpaRepository<RegistroPromessas, Long> {

    List<RegistroPromessas> findByProdutoIdOrderByDataAsc(String produtoId);
}
