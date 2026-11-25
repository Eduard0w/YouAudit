package br.com.youaudit.extracao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

/** Acesso ao banco da entidade {@link Afirmacao}. O Spring gera a implementação a partir da interface. */
public interface AfirmacaoRepository extends JpaRepository<Afirmacao, Long> {

    List<Afirmacao> findByAuditoriaId(Long auditoriaId);

    void deleteByAuditoriaId(Long auditoriaId);
}
