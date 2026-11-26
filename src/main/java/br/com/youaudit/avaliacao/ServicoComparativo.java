package br.com.youaudit.avaliacao;

import br.com.youaudit.baseline.VerificadorBaseline;
import br.com.youaudit.coleta.LimpadorTexto;
import br.com.youaudit.coleta.Origem;
import br.com.youaudit.coleta.Produto;
import br.com.youaudit.coleta.ProdutoRepository;
import br.com.youaudit.evento.Auditoria;
import br.com.youaudit.evento.AuditoriaRepository;
import br.com.youaudit.evento.CalculadoraHash;
import br.com.youaudit.evento.ItemDoEvento;
import br.com.youaudit.evento.ProdutoDoEvento;
import br.com.youaudit.evento.StatusAuditoria;
import br.com.youaudit.extracao.AfirmacaoDescartadaRepository;
import br.com.youaudit.extracao.AfirmacaoRepository;
import br.com.youaudit.extracao.TipoAfirmacao;
import br.com.youaudit.regras.Achado;
import br.com.youaudit.regras.AchadoRepository;
import br.com.youaudit.regras.TipoAchado;
import br.com.youaudit.simulador.ArquivoSimulado;
import br.com.youaudit.simulador.LeitorArquivoSimulado;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Monta o comparativo baseline × YouAudit. Só código; os rótulos vêm dos arquivos do Eduardo e não são alterados.
 *
 * <p><b>REAL (teste A, extração):</b> por página do gabarito (da divisão pedida), se a última auditoria está
 * CONCLUIDA: YouAudit = afirmações gravadas; baseline = {@link VerificadorBaseline} no MESMO texto. Comparação pelo
 * {@link ComparadorAfirmacoes}. Por padrão o tipo OUTRO fica fora da conta (os dois lados): é o "fora da lista" e o
 * baseline nunca o produz, então contar OUTRO mediria o rótulo e não a extração. Decisão a confirmar; o parâmetro
 * {@code incluirOutro} liga.
 *
 * <p><b>SIMULADO (teste B3, achados):</b> por produto do arquivo, a auditoria CONCLUIDA da página ORIGINAL (achada
 * pelo hash, para o reenvio corrigido do sim-001 não entrar no lugar). Unidade = par (produto, tipo de achado).
 */
@Service
public class ServicoComparativo {

    private final LeitorGabarito gabarito;
    private final LeitorArquivoSimulado simulados;
    private final ProdutoRepository produtos;
    private final AuditoriaRepository auditorias;
    private final AfirmacaoRepository afirmacoes;
    private final AfirmacaoDescartadaRepository descartadas;
    private final AchadoRepository achados;
    private final VerificadorBaseline baseline;
    private final LimpadorTexto limpador;
    private final CalculadoraHash hash;

    public ServicoComparativo(LeitorGabarito gabarito, LeitorArquivoSimulado simulados, ProdutoRepository produtos,
                              AuditoriaRepository auditorias, AfirmacaoRepository afirmacoes,
                              AfirmacaoDescartadaRepository descartadas, AchadoRepository achados,
                              VerificadorBaseline baseline, LimpadorTexto limpador, CalculadoraHash hash) {
        this.gabarito = gabarito;
        this.simulados = simulados;
        this.produtos = produtos;
        this.auditorias = auditorias;
        this.afirmacoes = afirmacoes;
        this.descartadas = descartadas;
        this.achados = achados;
        this.baseline = baseline;
        this.limpador = limpador;
        this.hash = hash;
    }

    @Transactional(readOnly = true)
    public Comparativo calcular(String divisao, boolean incluirOutro) {
        List<String> avisos = new ArrayList<>();
        avisos.add("Cerca de 60 itens não dão significância estatística: o resultado é um limite otimista (CONTEXTO.md §5).");
        Comparativo.Real real = real(divisao, incluirOutro, avisos);
        Comparativo.Simulado simulado = simulado(avisos);
        return new Comparativo(real, simulado, avisos);
    }

    private Comparativo.Real real(String divisao, boolean incluirOutro, List<String> avisos) {
        if (!gabarito.existe()) {
            avisos.add("Gabarito data/real/rotulos/afirmacoes.csv não encontrado: teste A não medido.");
            return null;
        }
        Map<String, List<RotuloAfirmacao>> porPagina = new TreeMap<>(gabarito.ler().stream()
                .filter(r -> r.origem() == Origem.REAL && r.divisao().equals(divisao))
                .collect(Collectors.groupingBy(RotuloAfirmacao::paginaId)));
        List<String> medidas = new ArrayList<>();
        List<String> semIa = new ArrayList<>();
        int noGabarito = 0, outroFora = 0, aceitos = 0, recusados = 0;
        int[] b = new int[3], y = new int[3];
        for (Map.Entry<String, List<RotuloAfirmacao>> e : porPagina.entrySet()) {
            Optional<Auditoria> ultima = auditorias.findFirstByProdutoIdOrderByVersaoDesc(e.getKey())
                    .filter(a -> a.getStatus() == StatusAuditoria.CONCLUIDA);
            if (ultima.isEmpty()) {
                semIa.add(e.getKey());
                continue;
            }
            Auditoria a = ultima.get();
            Produto p = a.getProduto();
            medidas.add(e.getKey());
            List<RotuloAfirmacao> ouro = e.getValue().stream().filter(r -> incluirOutro || r.tipo() != TipoAfirmacao.OUTRO).toList();
            outroFora += e.getValue().size() - ouro.size();
            noGabarito += ouro.size();

            List<AfirmacaoDetectada> daIa = afirmacoes.findByAuditoriaId(a.getId()).stream()
                    .map(x -> new AfirmacaoDetectada(x.getBlocoId(), x.getTipo(), x.getTrecho()))
                    .filter(x -> incluirOutro || x.tipo() != TipoAfirmacao.OUTRO).toList();
            List<AfirmacaoDetectada> doBaseline = baseline.verificar(new EntradaVerificacao(p.getId(), p.getOrigem(),
                    p.getPaginaTexto(), p.getPreco(), List.of())).afirmacoes().stream()
                    .filter(x -> incluirOutro || x.tipo() != TipoAfirmacao.OUTRO).toList();
            somar(b, ComparadorAfirmacoes.comparar(ouro, doBaseline));
            somar(y, ComparadorAfirmacoes.comparar(ouro, daIa));
            aceitos += afirmacoes.findByAuditoriaId(a.getId()).size();
            recusados += descartadas.findByAuditoriaId(a.getId()).size();
        }
        if (medidas.isEmpty()) {
            avisos.add("Nenhuma página REAL da divisão " + divisao + " tem auditoria CONCLUIDA: teste A não medido.");
        }
        return new Comparativo.Real(divisao, incluirOutro, medidas, semIa, noGabarito, outroFora,
                medidas.isEmpty() ? null : Metricas.de(b[0], b[1], b[2]),
                medidas.isEmpty() ? null : Metricas.de(y[0], y[1], y[2]),
                aceitos, recusados, "não medido: data/real/rotulos/achados_esperados.csv ainda não existe");
    }

    private Comparativo.Simulado simulado(List<String> avisos) {
        ArquivoSimulado arquivo;
        try {
            arquivo = simulados.ler();
        } catch (RuntimeException e) {
            avisos.add("Arquivo de produtos simulados indisponível: teste B3 não medido.");
            return null;
        }
        List<String> medidos = new ArrayList<>();
        List<String> semAuditoria = new ArrayList<>();
        Set<Comparativo.Par> plantados = new LinkedHashSet<>(), doYou = new LinkedHashSet<>(), doBase = new LinkedHashSet<>();
        for (ArquivoSimulado.ProdutoSimulado ps : arquivo.produtos()) {
            ProdutoDoEvento dados = ps.evento().produto();
            String texto = limpador.limpar(dados.paginaTexto());
            String hashOriginal = hash.sha256(texto);
            Optional<Auditoria> original = auditorias.findByProdutoIdOrderByVersaoAsc(dados.id()).stream()
                    .filter(a -> a.getStatus() == StatusAuditoria.CONCLUIDA && hashOriginal.equals(a.getHashPagina()))
                    .max(Comparator.comparingInt(Auditoria::getVersao));
            if (original.isEmpty()) {
                semAuditoria.add(dados.id());
                continue;
            }
            medidos.add(dados.id());
            ps.divergenciasPlantadas().forEach(t -> plantados.add(new Comparativo.Par(dados.id(), t)));
            achados.findByAuditoriaId(original.get().getId()).stream().map(Achado::getRegraCodigo).distinct()
                    .forEach(t -> doYou.add(new Comparativo.Par(dados.id(), t)));
            List<ItemEntrega> itens = dados.conteudo() == null || dados.conteudo().itens() == null ? List.of()
                    : dados.conteudo().itens().stream().map(ServicoComparativo::paraItem).toList();
            baseline.verificar(new EntradaVerificacao(dados.id(), Origem.SIMULADO, texto, dados.preco(), itens))
                    .achados().stream().map(AchadoDetectado::tipo).distinct()
                    .forEach(t -> doBase.add(new Comparativo.Par(dados.id(), t)));
        }
        if (medidos.isEmpty()) {
            avisos.add("Nenhum produto SIMULADO tem auditoria CONCLUIDA da página original: teste B3 não medido.");
            return new Comparativo.Simulado(medidos, semAuditoria, null, null, List.of(), List.of());
        }
        List<Comparativo.Par> soYou = plantados.stream().filter(doYou::contains).filter(x -> !doBase.contains(x)).toList();
        List<Comparativo.Par> soBase = plantados.stream().filter(doBase::contains).filter(x -> !doYou.contains(x)).toList();
        return new Comparativo.Simulado(medidos, semAuditoria, metricas(plantados, doBase), metricas(plantados, doYou), soYou, soBase);
    }

    private static Metricas metricas(Set<Comparativo.Par> plantados, Set<Comparativo.Par> achados) {
        int vp = (int) achados.stream().filter(plantados::contains).count();
        return Metricas.de(vp, achados.size() - vp, plantados.size() - vp);
    }

    private static void somar(int[] total, ComparadorAfirmacoes.Contagem c) {
        total[0] += c.vp();
        total[1] += c.fp();
        total[2] += c.fn();
    }

    private static ItemEntrega paraItem(ItemDoEvento i) {
        return new ItemEntrega(i.tipo(), i.titulo(), i.descricao());
    }
}
