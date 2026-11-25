package br.com.youaudit.coleta;

import java.util.List;

/** Resultado da leitura do arquivo de links: as linhas válidas e os NÚMEROS das inválidas (nunca o conteúdo). */
public record LeituraUrls(List<LinhaUrl> validas, List<Integer> linhasInvalidas) {
}
