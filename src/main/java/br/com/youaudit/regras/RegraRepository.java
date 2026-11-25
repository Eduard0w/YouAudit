package br.com.youaudit.regras;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

/** Acesso ao banco da entidade {@link Regra}. O Spring gera a implementação a partir da interface. */
public interface RegraRepository extends JpaRepository<Regra, Long> {

    Optional<Regra> findByCodigo(TipoAchado codigo);

    List<Regra> findByAtivaTrue();
}
