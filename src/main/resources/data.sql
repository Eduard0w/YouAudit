-- Regras e pesos iniciais (CONTRATOS.md §2). Os pesos são ILUSTRATIVOS e configuráveis:
-- para mudar a pontuação, mude aqui, nunca no código Java (mudança feita direto na tabela volta a este valor no próximo início).
-- fonte = 'hipótese de trabalho' porque nenhum peso foi validado com dados reais.
-- MERGE ... KEY(codigo): o banco é um arquivo e este script roda a cada início. Se a regra já existe, atualiza;
-- senão, insere. Com INSERT, o segundo início quebraria (codigo é único). Este arquivo é a fonte dos pesos.
MERGE INTO regra (codigo, descricao, peso, ativa, fonte) KEY (codigo) VALUES
('TERMO_PROIBIDO', 'Termo da lista de proibições', 30, TRUE, 'hipótese de trabalho'),
('PROMESSA_RESULTADO_SEM_RESSALVA', 'Promessa de resultado sem aviso de que resultados variam', 20, TRUE, 'hipótese de trabalho'),
('RECONHECIMENTO_SEM_COMPROVANTE', 'Reconhecimento oficial sem comprovante anexado', 25, TRUE, 'hipótese de trabalho'),
('QUANTIDADE_DIVERGENTE', 'Número na página diferente do cadastrado na entrega', 15, TRUE, 'hipótese de trabalho'),
('ELEMENTO_AUSENTE_NA_ENTREGA', 'Bônus ou certificado prometido que não existe na entrega', 25, TRUE, 'hipótese de trabalho'),
('CUSTO_OMITIDO', 'Custo exigido pelo conteúdo e não informado na página', 30, TRUE, 'hipótese de trabalho'),
('INFO_ESSENCIAL_AUSENTE', 'Falta preço, condições, prazo de acesso, canal de contato ou política de reembolso', 10, TRUE, 'hipótese de trabalho');
