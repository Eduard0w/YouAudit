package br.com.youaudit.extracao;

public record ResultadoValidacao(boolean valido, String motivo) {
    public static ResultadoValidacao ok() {return new ResultadoValidacao(true, null);}
    public static ResultadoValidacao invalido(String motivo) {return new ResultadoValidacao(false, motivo);}
}
