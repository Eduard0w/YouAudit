package br.com.youaudit.entrega;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Acesso ao banco da entidade {@link DescarteCruzamento}. */
public interface DescarteCruzamentoRepository extends JpaRepository<DescarteCruzamento, Long> {

    List<DescarteCruzamento> findByAuditoriaId(Long auditoriaId);

    void deleteByAuditoriaId(Long auditoriaId);
}
