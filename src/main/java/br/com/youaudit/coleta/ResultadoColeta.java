package br.com.youaudit.coleta;

import java.nio.file.Path;

/**
 * O que a coleta devolve: o texto limpo, o hash DESSE texto e a pasta do snapshot no disco.
 * O texto aqui é exatamente o que foi gravado em texto.txt.
 */
public record ResultadoColeta(String textoLimpo, String hash, Path pastaSnapshot) {
}
