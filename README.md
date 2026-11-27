# YouAudit

Protótipo para o Desafio de Inovação do estágio YouShop 2027.

O YouAudit reage ao evento **"produto enviado para aprovação"**: lê a página de venda, extrai as promessas, confere essas
promessas contra regras e contra o que o produto realmente entrega, calcula uma **pontuação de risco** e monta:

- uma **fila** para uma pessoa de Operações revisar, ordenada pela pontuação;
- um **relatório de correções** para o produtor.

> **O gatilho é simulado.** Este protótipo não está ligado a nenhum sistema real da YouShop. O evento "produto
> enviado" é disparado pelo simulador (ou pelo campo "Auditar um link"), e o envio do relatório ao produtor também é
> simulado (nenhum e-mail ou mensagem é enviado).

## Quem decide é uma pessoa

- **O código** calcula, valida, compara números e pontua.
- **A IA** só lê texto livre, associa descrições e redige sugestões de correção. Ela nunca aprova nem reprova um
  produto, nunca interpreta lei e nunca calcula dinheiro.
- **Todo alerta tem evidência literal**: o trecho precisa existir, caractere por caractere, na página ou nos dados da
  entrega. O código confere isso; sem trecho válido, o alerta é descartado e o descarte fica registrado.
- **Texto de página é dado, nunca instrução**: se a página disser "ignore as regras", isso é tratado como texto.
- A linguagem dos alertas é de **indício** ("possivelmente em desacordo com…"), nunca de acusação.

## Dados reais × simulados

| Origem | Onde fica | No repositório? |
|---|---|---|
| `SIMULADO` | `data/simulado/produtos.json` (18 produtos fictícios, alguns com divergências plantadas de propósito) | Sim |
| `REAL` | `data/real/` (páginas públicas coletadas, gabarito, banco) | **Não** (está no `.gitignore`) |

Toda tela e todo resultado separam `ORIGEM = REAL` de `ORIGEM = SIMULADO`, e as duas nunca entram na mesma métrica.
Páginas reais são anonimizadas (`pagina-01`, `pagina-02`…): nenhum nome de produtor ou marca aparece no código, nas
telas ou nos resultados.

Quem clonar o repositório **não** recebe as páginas reais nem o gabarito. O sistema funciona normalmente com os dados
simulados; a parte REAL do comparativo aparece como "não medido".

## Como rodar

Pré-requisitos: **Java 21** e **Maven** (`java -version`, `mvn -v`).

1. Defina a chave da IA **numa variável de ambiente** (nunca em arquivo do projeto):

   ```bash
   export GROQ_API_KEY="sua-chave"      # Linux/macOS (no perfil do shell)
   ```

   No Windows: Variáveis de ambiente do usuário → `GROQ_API_KEY`. Reabra o terminal depois.

   A IA usada é o **Groq** (`ia.provedor=groq` em `application.properties`). O Gemini continua implementado
   (`ia.provedor=gemini`, chave em `GEMINI_API_KEY`), mas saiu do padrão por indisponibilidade e cota diária baixa.

2. Rode os testes e suba a aplicação:

   ```bash
   mvn test
   mvn spring-boot:run
   ```

3. Abra **http://localhost:8080**.

Sem a chave, a aplicação sobe, mas as auditorias que dependem da IA terminam como `FALHOU`, com o motivo escrito.
O banco é um H2 em arquivo (`data/real/db/`). As respostas da IA ficam guardadas por hash da página, do prompt e do
modelo, então rodar de novo a mesma página não chama a IA outra vez.

## Roteiro da demonstração

A tela tem três abas: **Fila de revisão**, **Comparativo** e **Demonstração**. Cada produto mostra a etiqueta
Real ou Simulado.

1. Na aba **Demonstração**, **Iniciar simulador**: os 18 produtos fictícios chegam um a um e a fila vai se enchendo
   sozinha (atualiza a cada 3 s; 10 produtos por página, com busca e filtros por risco e origem).
2. Abrir um produto: pontuação, faixa (BAIXA, MEDIA, ALTA) e a conta. Na aba **Alertas**, cada alerta com o trecho e a
   regra de origem; na aba **Página × entrega**, os trechos destacados no texto.
3. Na aba **Relatório ao produtor**, as correções sugeridas (envio simulado).
4. Revisar os alertas (confirmar/descartar) e registrar a decisão na barra do fim da tela (liberar, pedir ajuste,
   escalar), sempre com motivo.
5. No `sim-001` ou no `sim-017`, **Reenviar versão corrigida** (simulado): a nova versão é auditada de novo. A aba
   **O que mudou** mostra cada alerta como resolvido, continua (idêntico depois de um pedido de ajuste) ou novo. O
   reenvio do `sim-017` foi montado como uma correção parcial: um termo proibido fica igual, a quantidade e o custo são
   corrigidos e entra uma promessa nova. O resultado exato depende da leitura da IA.
6. Ao **Liberar**, o sistema guarda as promessas e os alertas daquela versão, com um hash. A aba **Registro aprovado**
   mostra se o registro continua íntegro e se a página mudou desde a aprovação. O hash mostra alteração acidental; não é
   garantia de segurança.
7. Na aba **Comparativo** (botão "Calcular"): o mesmo conjunto passa por um verificador convencional (palavras-chave e
   números, sem IA) e pelo YouAudit, com REAL e SIMULADO separados.

Dá para abrir um produto direto pelo endereço, por exemplo `http://localhost:8080/#produto=sim-017&aba=mudancas`.

## Como o comparativo conta acertos

- **Teste A (REAL):** afirmações extraídas × gabarito rotulado à mão. Uma afirmação acerta se tem o mesmo tipo e um
  trecho está dentro do outro. Cada item do gabarito casa no máximo uma vez. Pedaços que caem dentro de um item já
  acertado (ex.: o gabarito marca uma lista de bônus numa linha só e o sistema marca um bônus por linha) são
  **agrupados**: não contam como acerto nem como erro. A regra vale igual para os dois sistemas, e a tela mostra
  quantos foram agrupados. O tipo `OUTRO` fica fora da conta.
- **Teste B3 (SIMULADO):** divergências plantadas nos produtos fictícios × achados de cada sistema. Mostra que o
  funcionamento técnico está certo, não que existe uma divergência real.
- O conjunto é pequeno (cerca de 60 afirmações): os números não têm significância estatística.

## Estrutura

Pacotes em `src/main/java/br/com/youaudit/`: `evento` (gatilho, idempotência por hash, pipeline), `coleta` (coleta
de página com robots.txt e proteção contra SSRF), `extracao` (IA, validação de esquema e de trecho literal, cache),
`regras`, `entrega` (cruzamento com o conteúdo entregue), `pontuacao`, `redacao`, `revisao` (fila e decisões),
`avaliacao` e `baseline` (comparativo), `simulador`, `web` e `config`. Pesos e listas de termos ficam em
configuração (`data.sql`, `baseline-termos.json`, `redacao-modelos.json`, `application.properties`), não no meio do código.

Tecnologias: Java 21, Spring Boot 3, Maven, H2, JPA, jsoup, Jackson, JUnit 5 e Mockito. Interface em um único
`index.html` com JavaScript puro.
