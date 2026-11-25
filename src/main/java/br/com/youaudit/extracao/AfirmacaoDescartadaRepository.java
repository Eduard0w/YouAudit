package br.com.youaudit.extracao;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Acesso ao banco da entidade {@link AfirmacaoDescartada}. */
public interface AfirmacaoDescartadaRepository extends JpaRepository<AfirmacaoDescartada, Long> {

    List<AfirmacaoDescartada> findByAuditoriaId(Long auditoriaId);

    void deleteByAuditoriaId(Long auditoriaId);
}
