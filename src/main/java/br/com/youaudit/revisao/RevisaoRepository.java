package br.com.youaudit.revisao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

/** Acesso ao banco da entidade {@link Revisao}. O Spring gera a implementação a partir da interface. */
public interface RevisaoRepository extends JpaRepository<Revisao, Long> {

    List<Revisao> findByAchadoId(Long achadoId);
}
