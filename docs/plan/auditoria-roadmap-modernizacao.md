# Auditoria e roadmap incremental de modernização

**Data da auditoria:** 2026-10-03

**Versão auditada:** 4.9.0 (`e4480d1`)

**Escopo:** itens catalogados em [`FUTURE_FEATURES.md`](../FUTURE_FEATURES.md),
código-fonte, scripts empacotados, build portátil e fluxos CLI essenciais.

## 1. Objetivo e método

Este documento é a fonte de verdade para o estado das melhorias futuras e para
sua execução. O catálogo original descreve a intenção; este roadmap registra o
que existe, as lacunas observadas e a sequência de entrega.

A classificação usa quatro estados:

- **Implementado:** o fluxo principal existe e corresponde ao comportamento
  descrito.
- **Parcial:** há código utilizável, mas faltam plataformas, semântica, testes ou
  parte relevante do requisito.
- **Incorreto:** a opção existe, mas executa comportamento diferente do
  prometido.
- **Ausente:** não foi encontrada implementação funcional.

A auditoria foi feita por inspeção estática, build com Java 25 e testes CLI de
versão, ajuda, MediaInfo, checksum e renomeação simulada. Integrações externas
não devem ser consideradas homologadas apenas porque compilam.

## 2. Resultado da auditoria

| ID | Item | Estado | Evidência e lacuna |
|---|---|---|---|
| AUD-01 | TheTVDB API v4 | **Ausente** | [`TheTVDBClient`](../../source/net/filebot/web/TheTVDBClient.java) usa `api.thetvdb.com`, autenticação da API legada e não implementa rotas `/v4`, PIN ou `user_key`. O teste CLI exibiu `404` no endpoint legado antes do fallback local. |
| AUD-02 | OpenSubtitles REST | **Ausente** | [`OpenSubtitlesXmlRpc`](../../source/net/filebot/web/OpenSubtitlesXmlRpc.java) e [`OpenSubtitlesClient`](../../source/net/filebot/web/OpenSubtitlesClient.java) continuam em XML-RPC; não há cliente para `api.opensubtitles.com/api/v1`. |
| AUD-03 | TMDb para séries | **Implementado** | [`TMDbTVClient`](../../source/net/filebot/web/TMDbTVClient.java) pesquisa séries, temporadas e episódios e está registrado em [`WebServices`](../../source/net/filebot/WebServices.java). |
| AUD-04 | Coleções TMDb | **Implementado** | `belongs_to_collection` é lido por [`TMDbClient`](../../source/net/filebot/web/TMDbClient.java) e exposto como `{collection}` por [`MediaBindingBean`](../../source/net/filebot/format/MediaBindingBean.java). |
| AUD-05 | AniDB e títulos alternativos | **Parcial** | Há `FloodLimit` e títulos localizados em [`AnidbClient`](../../source/net/filebot/web/AnidbClient.java), mas não há MyAnimeList nem modelo explícito para romaji, kanji e título oficial por idioma. |
| AUD-06 | `{plex}` | **Implementado** | [`PlexNamingStandard`](../../source/net/filebot/media/PlexNamingStandard.java) possui estratégia própria e o binding está exposto. |
| AUD-07 | `{kodi}`, `{emby}`, `{jellyfin}` | **Parcial** | Os bindings existem, porém todos delegam ao mesmo resultado de `{plex}` em [`MediaBindingBean`](../../source/net/filebot/format/MediaBindingBean.java). |
| AUD-08 | HDR e edição | **Implementado** | `{hdr}` reconhece HDR10, HDR10+, Dolby Vision e HLG; `{edition}` reconhece cortes a partir do nome do arquivo. Faltam testes dedicados. |
| AUD-09 | Metadados avançados de áudio/vídeo | **Parcial** | Existem `{vc}`, `{ac}`, `{audioLanguages}`, `{channels}` e `{audio}`, mas não os contratos literais `{videoCodec}` e `{audio.commercial}` descritos no catálogo. |
| AUD-10 | `--action clone` | **Parcial** | [`StandardRenameAction`](../../source/net/filebot/StandardRenameAction.java) usa reflink no Linux e clonefile no macOS; Windows/ReFS não é suportado e faltam testes por filesystem. |
| AUD-11 | `--action duplicate` | **Implementado** | Tenta hardlink e usa cópia como fallback. |
| AUD-12 | `--action test` | **Implementado** | Dry-run não altera o arquivo e foi validado em uma renomeação real via TheTVDB. |
| AUD-13 | `--apply prune` | **Parcial** | Remove diretórios vazios, mas não classifica nem remove `.nfo`, `.url`, samples ou outros resíduos. |
| AUD-14 | `--apply date` | **Incorreto** | [`ArgumentProcessor`](../../source/net/filebot/cli/ArgumentProcessor.java) grava a hora atual em vez da data de lançamento/exibição. |
| AUD-15 | `--apply artwork` | **Ausente** | Não é aceito pelo processador de pós-processamento; artwork só aparece nos scripts empacotados. |
| AUD-16 | `-exec` | **Implementado** | [`ExecCommand`](../../source/net/filebot/cli/ExecCommand.java) está integrado ao fluxo de processamento. |
| AUD-17 | AMC e notificações | **Parcial** | O `m1.jar.xz` inclui AMC com Plex, Emby, Kodi, Pushover e Pushbullet; não foram encontrados Discord, Telegram, webhook genérico ou Jellyfin explícito. O fonte não está neste repositório. |
| AUD-18 | Tema escuro | **Implementado** | [`SwingUI`](../../source/net/filebot/util/ui/SwingUI.java) usa FlatLaf, detecção Linux e preferência manual. |
| AUD-19 | HiDPI e SVG | **Parcial** | O runtime moderno fornece escala básica, mas não há política HiDPI testável nem migração dos ícones internos para SVG. |
| AUD-20 | Gerenciador de presets | **Parcial** | Criar, editar, salvar, excluir e aplicar existem em [`RenamePanel`](../../source/net/filebot/ui/rename/RenamePanel.java); importar e exportar não existem. |
| AUD-21 | Ferramentas no Filter | **Ausente** | O painel não possui lixeira segura, localizador de duplicatas ou detector de episódios ausentes. Há scripts separados para algumas dessas funções. |
| AUD-22 | Livre de licença e nagware | **Implementado com dívida** | Não há bloqueio de licença e `SupportDialog.maybeShow()` é no-op, mas o código morto e seus testes permanecem. |

### Achados transversais

- Existem chaves de serviços diretamente em `app.properties`. Mesmo quando são
  identificadores de cliente com uso público, a configuração deve suportar
  injeção externa, rotação e ausência segura.
- A resolução Ivy traz dependências antigas e grande árvore transitiva. Antes de
  ampliar integrações de rede, é necessário inventário de vulnerabilidades e
  uma política de atualização compatível.
- Algumas afirmações da página do projeto estão adiantadas em relação ao código,
  em especial `{kodi}`/`{emby}`/`{jellyfin}` e `--apply date`.
- A suíte atual possui testes para clientes legados, mas quase nenhum teste para
  os bindings e ações adicionados recentemente.

## 3. Princípios de execução

1. Cada tarefa deve produzir uma mudança pequena, revisável e reversível.
2. Um cliente novo convive temporariamente com o legado atrás de uma interface;
   o legado só é removido depois da homologação.
3. Integrações de rede devem ter testes unitários com fixtures locais e testes de
   contrato opcionais, nunca depender da rede na suíte padrão.
4. Operações de arquivos começam em dry-run e usam diretórios temporários nos
   testes. Nenhum teste destrutivo usa dados reais.
5. Documentação e ajuda CLI são atualizadas na mesma tarefa que muda o contrato.
6. Uma fase só termina quando seus critérios de saída forem atendidos.

## 4. Prioridades

| Prioridade | Resultado esperado |
|---|---|
| **P0 — Fundação e segurança** | Build reproduzível, testes confiáveis, configuração externa de credenciais e documentação verdadeira. |
| **P1 — Integrações essenciais** | TheTVDB v4 e OpenSubtitles REST substituem serviços descontinuados sem regressão offline. |
| **P2 — Correção da CLI** | Pós-processamento faz exatamente o que a ajuda promete e ações de filesystem são testadas. |
| **P3 — Naming e automação** | Bindings possuem contratos próprios e AMC oferece integrações modernas documentadas. |
| **P4 — Experiência gráfica** | Presets portáveis, HiDPI consistente e ferramentas de manutenção seguras. |
| **P5 — Limpeza estrutural** | Código morto removido e dependências antigas reduzidas após estabilização funcional. |

## 5. Plano incremental

### Fase 0 — Baseline confiável e configuração segura (P0)

#### T00 — Fixar a matriz de build e testes

**Dependências:** nenhuma.

- Documentar Java 25, Ant e Ivy suportados.
- Fazer `ant test` funcionar de forma repetível e separar testes unitários de
  contratos externos.
- Adicionar smoke tests para `-version`, `-help`, `-mediainfo`, `-check` e
  `--action test`.
- Publicar os mesmos comandos no CI.

**Aceite:** build limpo e suíte offline passam em duas execuções consecutivas;
falha de API externa não quebra a suíte unitária.

#### T01 — Externalizar credenciais e parâmetros dos provedores

**Dependências:** T00.

- Definir precedência: propriedade JVM, variável de ambiente, configuração do
  usuário e valor empacotado opcional.
- Remover segredos operacionais do artefato e documentar provisionamento local e
  no CI.
- Garantir mensagens claras quando uma credencial estiver ausente.
- Planejar rotação das chaves atualmente publicadas.

**Aceite:** nenhum segredo privado é necessário no repositório; clientes iniciam
sem credencial e falham de forma explícita apenas quando usados.

#### T02 — Corrigir a documentação pública

**Dependências:** nenhuma; pode avançar junto com T00.

- Marcar `{kodi}`, `{emby}` e `{jellyfin}` como aliases temporários de `{plex}`.
- Corrigir a descrição atual de `--apply date` e `--apply prune`.
- Publicar uma tabela de provedores e seu nível de suporte.

**Aceite:** README, índice, ajuda CLI e este roadmap não fazem promessas acima do
comportamento implementado.

### Fase 1 — Provedores de dados e legendas (P1)

#### T10 — Criar infraestrutura comum de clientes REST

**Dependências:** T00 e T01.

- Introduzir abstrações para JSON, cabeçalhos, autenticação, paginação, timeout,
  retry com backoff, rate limit e erros tipados.
- Permitir transporte falso para testes com fixtures.
- Não migrar provedores nesta tarefa.

**Aceite:** testes cobrem sucesso, 401, 404, 429, 5xx, timeout e JSON inválido.

#### T11 — Implementar autenticação TheTVDB v4

**Dependências:** T10.

- Implementar `POST /v4/login` com API key e PIN opcional.
- Armazenar token apenas em memória, com expiração e renovação sincronizada.
- Nunca registrar credenciais ou token.

**Aceite:** testes de fixture cobrem login, renovação e falha de autenticação.

#### T12 — Migrar pesquisa e catálogo TheTVDB v4

**Dependências:** T11.

- Migrar pesquisa, séries, temporadas, episódios, traduções e ordens oficiais.
- Mapear a resposta v4 para os modelos atuais sem alterar os bindings públicos.
- Preservar o índice local como fallback offline explícito.

**Aceite:** fixtures cobrem série comum, especiais, múltiplas temporadas e
tradução; renomeação CLI não apresenta chamadas ao endpoint legado.

#### T13 — Homologar e remover o cliente TheTVDB legado

**Dependências:** T12.

- Executar testes de contrato opt-in com credenciais de CI.
- Medir paridade do resultado em amostras conhecidas.
- Remover rotas, testes e configuração exclusivos da API antiga.

**Aceite:** nenhuma referência executável a `api.thetvdb.com` permanece.

#### T14 — Implementar OpenSubtitles REST: autenticação e pesquisa

**Dependências:** T10.

- Implementar `Api-Key`, `User-Agent`, login e bearer token opcional.
- Implementar pesquisa por hash, IMDb/TMDb e nome, com paginação e cotas.
- Manter DTOs REST separados dos modelos internos.

**Aceite:** pesquisa anônima e autenticada coberta por fixtures; limites de cota
são apresentados ao usuário sem retry agressivo.

#### T15 — Implementar download e homologar OpenSubtitles REST

**Dependências:** T14.

- Implementar solicitação de download, `.srt`/`.vtt` e validação de conteúdo.
- Integrar ao fluxo GUI/CLI atual.
- Remover XML-RPC somente após paridade; upload pode virar tarefa separada caso a
  API exija outro escopo.

**Aceite:** busca e download funcionam em teste de contrato opt-in; o endpoint
XML-RPC não é usado no fluxo normal.

#### T16 — Consolidar TMDb TV, AniDB e política MyAnimeList

**Dependências:** T00.

- Adicionar testes de regressão para TMDb TV e `{collection}`.
- Testar rate limit e idiomas AniDB.
- Definir ADR sobre integrar MyAnimeList diretamente ou enriquecer aliases via
  dados abertos; implementar apenas após a decisão.

**Aceite:** provedores já existentes têm contratos documentados e testes offline;
a decisão de MyAnimeList possui escopo e implicações de autenticação definidos.

### Fase 2 — Correção e segurança da CLI (P2)

#### T20 — Corrigir `--apply date`

**Dependências:** T00.

- Propagar data de lançamento/exibição até o pós-processamento.
- Definir fallback quando a data for desconhecida; não usar silenciosamente a
  hora atual.
- Preservar timezone e precisão suportada pelo filesystem.

**Aceite:** testes para filme, episódio, data ausente e filesystem sem suporte;
ajuda CLI corresponde ao resultado.

#### T21 — Tornar `--apply prune` conservador e configurável

**Dependências:** T00.

- Separar remoção de diretórios vazios da limpeza de resíduos.
- Definir allowlist explícita para resíduos e proteger mídia, legendas e arquivos
  desconhecidos.
- Adicionar dry-run e relatório do que seria removido.

**Aceite:** testes provam que arquivos não listados nunca são apagados e que o
dry-run não altera o diretório.

#### T22 — Implementar `--apply artwork`

**Dependências:** T12 ou T16, conforme o provedor escolhido.

- Extrair serviço reutilizável dos scripts de artwork.
- Definir política de nomes, conflitos, idioma e tipos de imagem.
- Integrar ao pipeline somente após renomeação bem-sucedida.

**Aceite:** fixtures validam seleção e nomes; testes de filesystem validam
conflito, repetição idempotente e falha parcial.

#### T23 — Cobrir ações de filesystem

**Dependências:** T00.

- Testar `move`, `copy`, `hardlink`, `clone`, `duplicate` e `test` em diretório
  temporário.
- Detectar capacidade de reflink antes da operação e produzir erro acionável.
- Criar ADR para suporte ou exclusão explícita de clone em Windows/ReFS.

**Aceite:** `duplicate` comprova hardlink quando possível e cópia no fallback;
`test` não grava; plataformas não suportadas são identificadas antes da ação.

#### T24 — Endurecer `-exec`

**Dependências:** T00.

- Documentar tokenização, escaping, execução paralela e códigos de saída.
- Adicionar testes com espaços, Unicode, metacaracteres e comando que falha.
- Garantir que argumentos sejam passados sem shell implícito.

**Aceite:** não há interpretação acidental por shell e falhas são propagadas.

### Fase 3 — Naming, metadados e AMC (P3)

#### T30 — Definir contratos de naming por servidor

**Dependências:** T00.

- Documentar diferenças reais entre Plex, Kodi, Emby e Jellyfin.
- Criar interface de estratégia comum e casos de exemplo versionados.
- Tratar filme, coleção, série, especial, multi-episódio e legenda.

**Aceite:** exemplos aprovados viram testes parametrizados antes da implementação.

#### T31 — Implementar `{kodi}`, `{emby}` e `{jellyfin}` independentes

**Dependências:** T30.

- Substituir aliases de `{plex}` por estratégias próprias.
- Manter `{plex}` compatível com formatos existentes.
- Cobrir caracteres inválidos e limites por plataforma.

**Aceite:** cada binding possui pelo menos um caso cujo resultado difere de Plex
e toda a matriz T30 passa.

#### T32 — Normalizar bindings de mídia avançados

**Dependências:** T00.

- Manter `{vc}`/`{ac}` por compatibilidade e adicionar aliases documentados como
  `{videoCodec}` se esse for o contrato desejado.
- Implementar classificação consistente para AV1, HEVC, AVC e VP9.
- Definir `{audio.commercial}` para Atmos, DTS-HD MA, TrueHD e E-AC-3.
- Adicionar testes para HDR, canais, idiomas e edição.

**Aceite:** fixtures MediaInfo produzem valores estáveis, sem depender apenas do
nome do arquivo quando metadados estão disponíveis.

#### T33 — Tornar o AMC auditável

**Dependências:** T00.

- Definir qual repositório é a fonte oficial dos scripts.
- Versionar fonte, testes e processo de geração do `m1.jar.xz`.
- Verificar assinatura ou checksum do pacote consumido pelo aplicativo.

**Aceite:** o pacote pode ser reproduzido a partir de fonte identificada e seu
hash é validado no build.

#### T34 — Completar notificações AMC

**Dependências:** T33.

- Criar adaptador comum de notificações.
- Implementar webhook genérico e, sobre ele, Discord e Telegram.
- Tratar Jellyfin explicitamente e preservar Plex, Emby, Kodi e Pushover.
- Mascarar tokens nos logs e aplicar timeouts.

**Aceite:** cada adaptador possui teste com servidor HTTP falso; falha de
notificação não desfaz uma organização já concluída.

### Fase 4 — Interface gráfica e manutenção (P4)

#### T40 — Importar e exportar presets

**Dependências:** T00 e contrato estável dos presets atuais.

- Definir formato JSON versionado e sem credenciais.
- Implementar exportação, importação, validação e resolução de nomes duplicados.
- Permitir pré-visualização antes de gravar preferências.

**Aceite:** round-trip preserva o preset; arquivo inválido não altera preferências;
versão futura gera erro legível.

#### T41 — Estabelecer política HiDPI

**Dependências:** T00.

- Inventariar ícones internos e tamanhos fixos.
- Testar escalas 100%, 150%, 200% e 300% em ao menos Linux e Windows.
- Migrar ícones prioritários para SVG ou variantes multirresolução.

**Aceite:** checklist visual não apresenta ícones borrados, cortes ou controles
inacessíveis nas escalas suportadas.

#### T42 — Adicionar “Mover para Lixeira” ao Filter

**Dependências:** T00.

- Usar a lixeira nativa por plataforma, nunca exclusão direta por padrão.
- Exibir quantidade, tamanho, confirmação e resultado parcial.
- Desabilitar a ação quando não houver backend seguro.

**Aceite:** teste por adaptador confirma destino recuperável; cancelamento e
falha parcial não perdem dados silenciosamente.

#### T43 — Localizador de duplicatas

**Dependências:** T42 para eventual remoção segura.

- Executar triagem por tamanho, depois hash forte; nunca nome apenas.
- Exibir grupos e permitir seleção manual.
- Manter análise separada da ação de remoção.

**Aceite:** arquivos de mesmo nome e conteúdo diferente não são classificados
como duplicados; nenhuma exclusão ocorre durante a análise.

#### T44 — Detector de episódios ausentes

**Dependências:** T12 e/ou T16.

- Comparar arquivos identificados com catálogo de episódios por temporada.
- Permitir ignorar especiais, episódios futuros e temporadas selecionadas.
- Mostrar origem e data dos dados.

**Aceite:** fixtures cobrem temporada completa, lacunas, especiais e episódios
ainda não exibidos.

### Fase 5 — Limpeza estrutural e dependências (P5)

#### T50 — Remover código morto de licença, doação e review

**Dependências:** T00.

- Confirmar ausência de chamadas em GUI, CLI e scripts.
- Remover `SupportDialog`, propriedades vazias e testes associados.
- Preservar apenas a tela “Sobre”, licença AGPL e créditos históricos.

**Aceite:** busca automatizada não encontra caminhos executáveis de licença ou
nagware; GUI inicia sem acesso de rede não solicitado.

#### T51 — Auditar e atualizar dependências

**Dependências:** T00; executar em lotes após Fase 1.

- Gerar SBOM e relatório de vulnerabilidades da árvore Ivy e binários nativos.
- Remover dependências transitivas não usadas e artefatos `sources`/`javadoc` do
  fluxo de build.
- Atualizar em lotes pequenos com testes e notas de compatibilidade.

**Aceite:** SBOM é gerado no CI; vulnerabilidades críticas possuem correção ou
exceção documentada com prazo.

## 6. Ordem de entrega sugerida

```text
T00 ─┬─> T01 ─> T10 ─┬─> T11 ─> T12 ─> T13
     │                └─> T14 ─> T15
     ├─> T20 ─> T21 ─> T23 ─> T24
     ├─> T30 ─> T31 ─> T32
     └─> T40 ─> T41

T12/T16 ─> T22
T00 ─> T33 ─> T34
T12/T16 ─> T44
T42 ─> T43
T00 ─> T50
Fase 1 estabilizada ─> T51
```

### Marcos incrementais

1. **M0 — Baseline confiável:** T00–T02.
2. **M1 — Metadados sustentáveis:** T10–T16.
3. **M2 — CLI correta e segura:** T20–T24.
4. **M3 — Naming e automação modernos:** T30–T34.
5. **M4 — GUI produtiva:** T40–T44.
6. **M5 — Base limpa e sustentável:** T50–T51.

## 7. Definition of Done por tarefa

Uma tarefa só pode ser concluída quando:

- código e documentação foram atualizados juntos;
- testes unitários relevantes passam offline;
- integrações externas possuem fixtures e, quando aplicável, teste de contrato
  opt-in;
- erros não expõem tokens, senhas ou dados sensíveis;
- operações de arquivos possuem teste de falha e rollback ou comportamento
  idempotente documentado;
- `ant test`, `ant fatjar` e o smoke test CLI passam;
- não há regressão no fallback offline;
- a matriz de auditoria deste documento foi atualizada.

## 8. Próxima tarefa recomendada

Começar por **T00 — Fixar a matriz de build e testes**. Ela reduz o risco de todas
as fases posteriores e cria a base necessária para substituir APIs sem depender
de testes manuais ou da disponibilidade momentânea dos provedores.
