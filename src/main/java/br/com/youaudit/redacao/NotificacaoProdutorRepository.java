package br.com.youaudit.redacao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

/** Acesso ao banco da entidade {@link NotificacaoProdutor}. O Spring gera a implementação a partir da interface. */
public interface NotificacaoProdutorRepository extends JpaRepository<NotificacaoProdutor, Long> {

    List<NotificacaoProdutor> findByAuditoriaId(Long auditoriaId);

    void deleteByAuditoriaId(Long auditoriaId);
}
