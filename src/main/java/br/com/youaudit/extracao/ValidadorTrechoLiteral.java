package br.com.youaudit.extracao;

import org.springframework.stereotype.Component;

@Component
public class ValidadorTrechoLiteral {

    // compara caractere por caractere, sem normalizar
    public ResultadoValidacao validar(String trecho, String textoFonte) {
        if (trecho == null || trecho.isBlank()) {
            return ResultadoValidacao.invalido("trecho vazio");
        }

        if (textoFonte == null) {
            return ResultadoValidacao.invalido("fonte inexistente");
        }

        if (!textoFonte.contains(trecho)) {
            return ResultadoValidacao.invalido("trecho não encontrado literalmente na fonte");
        }

        return ResultadoValidacao.ok();
    }

}
