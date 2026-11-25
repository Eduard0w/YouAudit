package br.com.youaudit.coleta;

/**
 * Não foi possível obter a página pela URL (robots.txt proíbe, erro HTTP, tempo esgotado...).
 * A mensagem vai para o banco como motivo da falha, por isso NUNCA inclui a URL:
 * uma URL real pode ter o nome do produtor (regra 8 do CLAUDE.md).
 */
public class ColetaFalhouException extends RuntimeException {

    public ColetaFalhouException(String motivo) {
        super(motivo);
    }
}
