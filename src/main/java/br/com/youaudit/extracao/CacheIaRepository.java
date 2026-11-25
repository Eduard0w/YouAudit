package br.com.youaudit.extracao;

import org.springframework.data.jpa.repository.JpaRepository;

/** Acesso ao banco da entidade {@link CacheIa}. O Spring gera a implementação a partir da interface. */
public interface CacheIaRepository extends JpaRepository<CacheIa, String> {
}
