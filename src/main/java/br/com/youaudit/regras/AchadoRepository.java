package br.com.youaudit.regras;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

/** Acesso ao banco da entidade {@link Achado}. O Spring gera a implementação a partir da interface. */
public interface AchadoRepository extends JpaRepository<Achado, Long> {

    List<Achado> findByAuditoriaId(Long auditoriaId);
}
