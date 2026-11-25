package br.com.youaudit.coleta;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LeitorRobotsTest {

    private static final String ROBO = "YouAudit-prototipo";
    private final LeitorRobots leitor = new LeitorRobots();

    @Test
    void semRobotsPermiteTudo() {
        assertThat(leitor.permite("", ROBO, "/curso")).isTrue();
    }

    @Test
    void disallowProibePorPrefixo() {
        String robots = "User-agent: *\nDisallow: /privado";

        assertThat(leitor.permite(robots, ROBO, "/privado/aula")).isFalse();
        assertThat(leitor.permite(robots, ROBO, "/curso")).isTrue();
    }

    @Test
    void disallowVazioNaoProibeNada() {
        assertThat(leitor.permite("User-agent: *\nDisallow:", ROBO, "/qualquer")).isTrue();
    }

    @Test
    void regraMaisLongaVence() {
        String robots = "User-agent: *\nDisallow: /cursos\nAllow: /cursos/publico";

        assertThat(leitor.permite(robots, ROBO, "/cursos/publico/loja")).isTrue();
        assertThat(leitor.permite(robots, ROBO, "/cursos/outro")).isFalse();
    }

    @Test
    void grupoComONomeDoNossoRoboPrevaleceSobreOAsterisco() {
        String robots = """
                User-agent: *
                Disallow: /

                User-agent: youaudit-prototipo
                Allow: /
                """;

        assertThat(leitor.permite(robots, ROBO, "/curso")).isTrue();
        assertThat(leitor.permite(robots, "OutroRobo", "/curso")).isFalse();
    }

    @Test
    void grupoDeOutroRoboNaoNosAfeta() {
        String robots = "User-agent: Googlebot\nDisallow: /";

        assertThat(leitor.permite(robots, ROBO, "/curso")).isTrue();
    }

    @Test
    void variosUserAgentSeguidosFormamUmGrupoSo() {
        String robots = "User-agent: robo-a\nUser-agent: youaudit-prototipo\nDisallow: /checkout";

        assertThat(leitor.permite(robots, ROBO, "/checkout/1")).isFalse();
    }

    @Test
    void entendeAsteriscoECifrao() {
        String robots = "User-agent: *\nDisallow: /*.pdf$\nDisallow: /*?sessao=";

        assertThat(leitor.permite(robots, ROBO, "/material/apostila.pdf")).isFalse();
        assertThat(leitor.permite(robots, ROBO, "/material/apostila.pdf?v=2")).isTrue();
        assertThat(leitor.permite(robots, ROBO, "/curso?sessao=abc")).isFalse();
    }

    @Test
    void ignoraComentariosEMaiusculasNasDiretivas() {
        String robots = "# comentário\nUSER-AGENT: *   # todos\nDISALLOW: /admin # área restrita";

        assertThat(leitor.permite(robots, ROBO, "/admin")).isFalse();
    }
}
