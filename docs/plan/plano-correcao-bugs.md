# Plano de correção de bugs e falhas

**Data:** 2026-10-03

**Base:** [`auditoria-bugs-falhas.md`](auditoria-bugs-falhas.md) (BUG-01 a BUG-24)

**Relacionado:** [`auditoria-roadmap-modernizacao.md`](auditoria-roadmap-modernizacao.md)
(tarefas T00–T51)

## 1. Objetivo

Corrigir os defeitos registrados na auditoria, nesta ordem: proteção de dados,
segurança, regressões da GUI, correção funcional e base de qualidade. Cada
correção é uma tarefa `FIX-xx` pequena, revisável e reversível. Ela só termina
quando existe um teste automatizado que reproduz a falha original e passa sem
depender de rede.

## 2. Regras de execução

1. **Teste antes da correção.** Cada FIX começa com um teste que falha,
   reproduzindo a PoC da auditoria, e termina com o mesmo teste passando.
2. **Uma branch e um PR por FIX**, ou por grupo pequeno indicado no plano, com
   o nome `fix/<id>-<resumo>`.
3. **Sem rede na suíte offline.** Testes de filesystem usam `TemporaryFolder`
   do JUnit; testes de integração usam fixtures locais.
4. **Nada destrutivo sem dry-run.** Qualquer código que apague ou mova
   arquivos precisa respeitar `--action test` e ter teste que comprove isso.
5. **Documentação junto.** Se a correção muda o comportamento visível (CLI,
   ajuda, atalhos), a documentação é atualizada no mesmo PR.
6. **Atualizar a auditoria.** Ao concluir, marcar o BUG como **Corrigido** em
   `auditoria-bugs-falhas.md`, com o hash do commit.

## 3. Visão geral

| Fase | Tema | Tarefas | BUGs |
|---|---|---|---|
| **C0** | Rede de proteção de testes | FIX-00 | BUG-19 (parcial) |
| **C1** | Proteção de dados (urgente) | FIX-01 a FIX-04 | BUG-01, 02, 03, 07, 18 |
| **C2** | Segurança e integridade | FIX-05 a FIX-09 | BUG-05, 08, 11, 13, 15 |
| **C3** | Regressões da GUI | FIX-10 a FIX-13 | BUG-04, 10, 14, 17, 20 |
| **C4** | Correção funcional | FIX-14 a FIX-18 | BUG-06, 09, 12, 16, 21 |
| **C5** | Qualidade e manutenção | FIX-19 a FIX-22 | BUG-19, 22, 23, 24, 25 |

```text
FIX-00 ─┬─> FIX-01 ─> FIX-15 (date usa a mesma infraestrutura do pós-processamento)
        ├─> FIX-02 ─> FIX-04
        ├─> FIX-03
        ├─> FIX-04
        ├─> C2 (FIX-05 … FIX-09)
        ├─> FIX-10 ─> FIX-11 (o cancelamento depende do novo fluxo assíncrono)
        └─> C4 / C5

FIX-06 (ADR do sandbox) ─> decisão registrada antes de alterar código
FIX-19 depende de FIX-00 e conclui BUG-19
```

As tarefas da fase C1 podem andar em paralelo depois de FIX-00.

## 4. Tarefas

### Fase C0 — Rede de proteção

#### FIX-00 — Separar a suíte offline e torná-la bloqueante

**Resolve:** BUG-19 (parcial) · **Relaciona:** T00 · **Status:** concluída na
branch `fix/00-suite-offline`

- Criar `OfflineTests` reunindo as suítes que não usam rede (`HistoryTest`,
  `ExpressionFormatTest`, `SimilarityTestSuite`, `UtilTestSuite`,
  `SubtitleReaderTestSuite`, `MediaDetectionTest` etc.).
- Mover `WebTestSuite` para um alvo separado, `ant test-online`, opcional e não
  bloqueante.
- No alvo `ant test`, rodar apenas a suíte offline com `haltonfailure="yes"` e
  `failureproperty`, fazendo o build falhar quando um teste falhar.
- Criar um utilitário de teste para diretório temporário e para a cópia isolada
  de `ApplicationFolder.AppData`, usando a propriedade `application.dir`.

**Aceite:** `ant test` passa duas vezes seguidas sem rede, e uma falha proposital
quebra o build.

### Fase C1 — Proteção de dados

#### FIX-01 — Reescrever `--apply prune` de forma conservadora

**Resolve:** BUG-01 · **Relaciona:** T21 · **Status:** concluída na branch
`fix/01-apply-prune`

> **Decisões de implementação:** "arquivo movido" é um arquivo de entrada que
> não existe mais na origem depois do rename. Uma pasta só pode ser removida se
> estiver dentro de uma **pasta** de entrada; quando a entrada é um arquivo
> isolado, nada é removido. A remoção de resíduos (`.nfo`, `.url`, samples)
> continua fora do escopo (T21). `--apply date` continua gravando a hora atual
> até a FIX-15, mas já não roda em `--action test`.

- Não executar o pós-processamento quando a ação for `test`; apenas registrar o
  que seria feito.
- Executar `prune` somente para `move` e `keeplink`.
- Considerar apenas as pastas de origem dos arquivos **efetivamente movidos**,
  subindo até a raiz de entrada informada, sem ultrapassá-la e sem descer em
  subpastas que não tenham relação com esses arquivos.
- Não seguir symlinks (`Files.isDirectory(p, NOFOLLOW_LINKS)`); trocar a
  recursão por iteração.
- Tratar `Thumbs.db` e `.DS_Store` como "vazio", reaproveitando
  `isThumbnailStore`, como a GUI já faz.

**Testes:** a PoC da auditoria (pasta vazia vizinha preservada), `--action test`
sem alterações, `copy` sem prune, symlink para fora da árvore preservado e laço
de symlink sem `StackOverflowError`.

**Aceite:** nenhum diretório fora do caminho dos arquivos movidos é removido.

#### FIX-02 — Histórico resistente a corrupção e com gravação atômica

**Resolve:** BUG-02 (e BUG-18) · **Status:** concluída na branch
`fix/02-historico-atomico`

> **Decisões de implementação:** um arquivo vazio continua sendo um histórico
> vazio válido. Entradas sem `from`/`to` são ignoradas com aviso, em vez de
> invalidar o arquivo. `getCompleteHistory` propaga o erro, sem criar backup;
> só o `commit` move o arquivo corrompido. O CLI passou a fazer commit
> explícito antes de sair. A parte de streams do `HistoryDialog` prevista na
> FIX-04 foi antecipada.

- `History.importHistory`: lançar exceção tipada, por exemplo
  `HistoryFormatException`, em vez de devolver vazio. Manter um método
  tolerante só onde ele for explicitamente necessário.
- `HistorySpooler.commit`: se a leitura falhar, renomear o arquivo para
  `history.xml.corrupt-<timestamp>`, avisar em `log.warning` e só depois gravar
  um novo arquivo.
- `exportHistory`: propagar `IOException`/`TransformerException`.
- Gravar em `history.xml.tmp` e aplicar `Files.move(..., ATOMIC_MOVE,
  REPLACE_EXISTING)`, mantendo o `FileLock` em um arquivo `.lock` separado.
- Suprimir o `[Fatal Error]` do parser no stderr com um `ErrorHandler` próprio.

**Testes:** XML truncado preserva o original e cria o backup; falha simulada de
escrita mantém o arquivo anterior intacto; ida e volta (*round-trip*) com
Unicode e caminhos absolutos.

**Aceite:** a PoC da auditoria não perde as entradas antigas.

#### FIX-03 — Bloquear *path traversal* na extração

**Resolve:** BUG-03 · **Status:** concluída na branch `fix/03-zip-slip`

> **Decisões de implementação:** a validação é léxica (`normalize` +
> `startsWith`), acrescida de uma checagem de `toRealPath` no momento da
> escrita. `\` é tratado como separador em qualquer plataforma. `.`, `a/..`
> e caminhos vazios também são rejeitados. Os testes do extrator nativo usam
> `lib/native/linux-amd64` via `java.library.path` e são pulados se a
> biblioteca não carregar; os do `7z` são pulados se o executável não existir.

- Em `FileMapper.getOutputFile`, normalizar `outputDir.toPath().resolve(entry)`
  e rejeitar o resultado se ele não começar com `outputDir` normalizado, além
  de rejeitar caminhos absolutos e letras de unidade.
- Aplicar a mesma validação a `SevenZipExecutable`, ao listar entradas antes de
  extrair, e conferir o comportamento de `ApacheVFS`.
- Registrar e pular a entrada maliciosa, continuando com as demais.

**Testes:** zip com `../../x`, `/etc/x`, `..\\x` (estilo Windows) e entrada
legítima com `..` no nome do arquivo (por exemplo `a..b.txt`), nos três
extratores, quando o binário nativo estiver disponível.

**Aceite:** nenhuma entrada é escrita fora da pasta de saída.

#### FIX-04 — Lixeira segura, `-revert` headless e streams do histórico

**Resolve:** BUG-07, BUG-18 · **Relaciona:** T42 · **Status:** concluída na
branch `fix/04-lixeira-revert-headless`

> **Decisões de implementação:** a lixeira de outras partições segue a
> especificação (`$topdir/.Trash-$uid`, com permissão 700) e cai para a
> lixeira da home quando não é possível criá-la. A propriedade
> `-Dnet.filebot.trash.home` permite redirecionar a lixeira (usada para isolar
> os testes). A opção `--trash=never|auto` na CLI **não** foi implementada: com
> a lixeira funcionando em headless, não há caso de uso que justifique uma nova
> opção agora. Os testes rodam sempre com `java.awt.headless=true`.

- `UserFiles.trash`: verificar `GraphicsEnvironment.isHeadless()` e
  `Desktop.isDesktopSupported()` antes de usar `Desktop`.
- Implementar a lixeira XDG no Linux (`$XDG_DATA_HOME/Trash/files` + `info`,
  com `.trashinfo`), usada em headless e quando o AWT não oferecer suporte.
- Quando não houver lixeira, registrar `log.warning` antes de excluir de forma
  permanente. Avaliar uma opção `--trash=never|auto` na CLI.
- ~~`HistoryDialog`: usar `try-with-resources` na importação e na exportação, e
  mostrar o erro ao usuário~~ (feito na FIX-02).

**Testes:** `revert` de cópia em JVM headless (`-Djava.awt.headless=true`); a
lixeira XDG gera `.trashinfo` válido; importar um arquivo inválido exibe erro.

**Aceite:** a PoC de BUG-07 reverte sem exceção e o arquivo vai para a lixeira.

### Fase C2 — Segurança e integridade

#### FIX-05 — Integridade do pacote de scripts

**Resolve:** BUG-05 · **Relaciona:** T33 · **Status:** concluída na branch
`fix/05-integridade-scripts`

> **Decisões de implementação:** a assinatura antiga do pacote (de 2018, do
> autor original) não vale mais desde que o `amc.groovy` foi alterado, então a
> confiança passou a ser um hash fixado na aplicação, e não uma assinatura. O
> hash é do jar descompactado, o que torna a verificação independente da
> compressão. Uma atualização do pacote de scripts exige atualizar
> `downloads/scripts/m1.jar.xz` e `script.bundle.sha256` juntos; o
> `ScriptBundleTest` falha se eles divergirem. Os canais `dev:` e URL remota
> explícita continuam sem verificação, porque são escolhas explícitas do
> usuário para desenvolvimento.

- Remover os caminhos relativos ao CWD (`downloads/scripts`,
  `../downloads/scripts`) de `ScriptSource`.
- Validar o SHA-256 do `m1.jar.xz` contra um valor publicado junto com o build
  (`app.properties` ou um manifesto assinado), tanto para o recurso embutido
  quanto para o download.
- Se a validação falhar, recusar a execução com uma mensagem clara.
- Remover o parâmetro de certificado sem uso de `ScriptBundle` ou reativar a
  verificação com um certificado próprio do projeto.

**Testes:** pacote adulterado é rejeitado; o pacote legítimo executa
`fn:sysinfo`; uma pasta de trabalho com `downloads/scripts` falso é ignorada.

#### FIX-06 — Decidir e documentar o modelo de segurança das expressões

**Resolve:** BUG-08 · **Status:** concluída na branch `fix/06-sandbox-expressoes`

- Escrever um ADR (`docs/adr/0001-expressoes-de-formato.md`) com as opções:
  **(a)** aceitar a execução com privilégios totais e documentar o risco de
  colar formatos de terceiros; **(b)** restringir com `SecureASTCustomizer` do
  Groovy (whitelist de receivers, imports e chamadas estáticas);
  **(c)** `groovy-sandbox` (Jenkins) em tempo de execução + `TimedInterrupt`.
- ADR aceito com a **opção C**. Sandbox implementado em `ExpressionSandbox` +
  `SandboxedCompiledScript`; remoção de `Policy`/`SecurityManager`/
  `AccessController`; `SecureCompiledScript` e `PrivilegedInvocation` removidos.

**Aceite:** ADR aprovado; zero avisos `[removal]` relacionados; testes do sandbox
(`ExpressionSandboxTest`) cobrem processos, rede, reflexão, `metaClass`,
`System.exit` e limite de tempo.

#### FIX-07 — Remover a heurística de Groovy inline

**Resolve:** BUG-11 · **Status:** concluída na branch `fix/07-groovy-inline`

- `INLINE_GROOVY` passa a aceitar apenas o prefixo `g:`.
- Atualizar a ajuda e a documentação, se o comportamento heurístico tiver sido
  divulgado.

**Testes:** um caminho inexistente com espaço gera erro de "script não
encontrado"; `g:println 1` continua funcionando.

#### FIX-08 — AniDB e endpoints em HTTPS

**Resolve:** BUG-13 · **Relaciona:** T16 · **Status:** concluída na branch `fix/08-https-endpoints`

- Confirmar o endpoint atual do índice de títulos AniDB e sua política
  (HTTPS, *User-Agent*, cliente registrado, limite de downloads diários).
- Migrar AniDB, AcoustID, OMDb e TVMaze para `https://`.
- Quando o índice remoto não puder ser baixado, usar `anidb.txt.xz` local como
  fallback e registrar um aviso visível.

**Testes:** fixtures do índice de títulos; teste de contrato opcional no alvo
`test-online`.

#### FIX-09 — `-exec` falha fechado

**Resolve:** BUG-15 · **Relaciona:** T24 · **Status:** concluída na branch `fix/09-exec-falha-fechada`

- Se qualquer argumento do template falhar, abortar aquele comando com
  `log.warning` mostrando a expressão e o motivo, sem remover o argumento.
- No modo sequencial, uma falha em um comando não interrompe os comandos
  seguintes (cada comando é independente por arquivo).

**Testes:** template com binding inexistente não executa nada; espaços e
Unicode nos argumentos são preservados.

### Fase C3 — Regressões da GUI

#### FIX-10 — Rename e Match assíncronos

**Resolve:** BUG-04 · **Status:** concluída na branch `fix/10-rename-match-async`

- Substituir `runTask(...).get()` na EDT por um fluxo assíncrono:
  `ProgressMonitor.runTask` devolve o *future* e a continuação (atualizar o
  modelo, gravar o histórico, remover pastas vazias) roda em um callback na
  EDT.
- Desabilitar as ações Rename e Match enquanto houver tarefa em andamento.

**Testes:** teste com `SwingUtilities.invokeAndWait` provando que a EDT não fica
bloqueada (*watchdog*), mais um checklist manual com uma cópia de 5 GB.

**Aceite:** o diálogo de progresso aparece após 500 ms e o botão Cancel
responde.

#### FIX-11 — `ProgressMonitor` correto

**Resolve:** BUG-10 · **Depende:** FIX-10 · **Status:** concluída na branch `fix/11-progress-monitor`

- Guardar a `Thread` executora e interrompê-la em `cancel(true)`, ou usar
  `FutureTask.run()` com um `Callable` adaptado.
- O worker devolve o próprio `renameLog`, sem compartilhar um mapa mutável com a
  EDT. No cancelamento, aguardar o término do worker antes de ler o resultado
  parcial.
- Normalizar o progresso para uma escala de 0 a 1000 com `long`, evitando o
  limite de `Integer.MAX_VALUE`.

**Testes:** o cancelamento interrompe um worker bloqueado em `sleep`; o
progresso de 3 GiB de 5 GiB mostra cerca de 60%.

#### FIX-12 — Atalhos e ajuda coerentes

**Resolve:** BUG-14 · **Status:** concluída na branch `fix/12-atalhos-ajuda`

- Corrigir os textos de `FileBotMenuBar` e `GettingStartedStage`.
- Adicionar uma confirmação ao `Ctrl+Shift+Delete` (limpar cache).
- Decidir se F5 continua abrindo o GroovyPad. Se o atalho de rename for
  desejado, criar uma tarefa separada.

**Aceite:** todo atalho documentado corresponde ao comportamento real.

#### FIX-13 — GroovyPad e detecção de tema

**Resolve:** BUG-17, BUG-20 · **Status:** concluída na branch `fix/13-groovypad-tema`

- GroovyPad: remover `Thread.stop()`. Cancelar por interrupção e, para scripts
  que ignoram interrupção, rodar o script com `ThreadInterrupt` do Groovy
  (AST transform `@ThreadInterrupt` via `CompilerConfiguration`).
- Tema: executar a detecção fora da EDT, ou antes de criar a UI no `main`, com
  `waitFor(2, SECONDS)` e descarte do stderr. Consultar o portal também quando
  o GNOME responder `'default'`.

**Testes:** cancelar `while(true){}` termina em até 2 s; a detecção com um
`gsettings` falso e lento não passa de 2 s.

### Fase C4 — Correção funcional

#### FIX-14 — Launcher da raiz

**Resolve:** BUG-06 · **Status:** concluída na branch `fix/14-launcher`

- Fazer o `ant portable` gerar também o link ou cópia `dist/portable` sem
  versão; o `./filebot` da raiz aponta para lá, ou resolve o diretório
  `FileBot_*-portable` mais recente.

**Aceite:** `./filebot -version` funciona depois de `ant portable`.

#### FIX-15 — `--apply date` correto e seguro

**Resolve:** BUG-09, AUD-14 · **Depende:** FIX-01 · **Relaciona:** T20 · **Status:** concluída na branch `fix/15-apply-date`

- Usar a data de lançamento ou exibição do objeto identificado (`Movie`,
  `Episode`), propagada a partir de `renameAll`.
- Se a data for desconhecida, não alterar nada e registrar o motivo.
- Não alterar o mtime quando a ação for `symlink`, `hardlink` ou `duplicate`
  com hardlink. No caso de symlink, alterar apenas o link, se for suportado.

**Testes:** filme, episódio, data ausente, symlink e hardlink (o original
mantém o mtime).

#### FIX-16 — Fonte de dados do `ReleaseInfo`

**Resolve:** BUG-12 · **Status:** concluída na branch `fix/16-release-info`

- Nova ordem: override `-Durl.*` → cache remoto válido → dados locais
  (`AppData/data`, `application.dir/data`, `/opt/filebot/data`) → recurso
  embutido como último fallback.
- Remover os caminhos relativos ao CWD.
- Registrar em `log.warning`, e não só em `FINE`, quando um índice ficar vazio.

**Testes:** o override por propriedade tem precedência; a ausência de rede usa
o recurso embutido; uma pasta de trabalho com dados falsos é ignorada.

#### FIX-17 — Regex de `{hdr}` e `{edition}`

**Resolve:** BUG-16 · **Relaciona:** T32 · **Status:** concluída na branch `fix/17-hdr-regex`

- Trocar `\b` por delimitadores explícitos (`(?<![A-Za-z0-9])` e
  `(?![A-Za-z0-9])`).
- Restringir `DV` a contextos de vídeo (por exemplo `DV.HDR`, `DoVi`, `DV.HEVC`)
  para reduzir falsos positivos.
- Detectar HLG também por `transfer_characteristics` do MediaInfo.

**Testes:** tabela parametrizada de nomes de arquivo, com HDR10+, HDR10Plus,
DV, DoVi, HLG, "DVDRip" e casos sem HDR.

#### FIX-18 — `--conflict index`

**Resolve:** BUG-21 · **Status:** concluída na branch `fix/18-conflict-index`

- Sem extensão ou para pastas, gerar `Nome.1`, `Nome.2`, ….
- Remover o limite fixo de 99 ou falhar com `CmdlineException` legível.

**Testes:** arquivo sem extensão, pasta e mais de 100 conflitos.

### Fase C5 — Qualidade e manutenção

#### FIX-19 — Testes de serviços externos com fixtures

**Resolve:** BUG-19 (conclusão) · **Depende:** FIX-00 · **Relaciona:** T10, T16 · **Status:** concluída na branch `fix/19-fixtures`

- Gravar respostas reais como fixtures (`test/resources/web/<serviço>/…`) para
  TMDb, TVMaze, OMDb, AniDB e AcoustID.
- Adaptar os testes para validar o *parsing* sobre as fixtures; asserções sobre
  dados vivos ficam só em `test-online`.
- Marcar como `@Ignore` documentado os testes de TheTVDB legado e OpenSubtitles
  XML-RPC até as migrações T11–T15.
- Corrigir o `DateMetricTest`, que deve esperar `-1` para datas diferentes, e
  incluí-lo em `OfflineTests` (BUG-25). **Concluído na branch `fix/23-date-metric-test`.**

**Aceite:** nenhuma das 37 falhas atuais aparece em `ant test`.

#### FIX-20 — Preferências com gravação atômica

**Resolve:** BUG-22 · **Status:** concluída na branch `fix/20-prefs-atomico`

- Em `flush`, gravar em um arquivo temporário e aplicar `ATOMIC_MOVE`.
- Em `sync`, ignorar e registrar chaves malformadas em vez de abortar a carga.
- Se a carga falhar, não gravar por cima no shutdown e preservar um backup.

**Testes:** arquivo com uma chave sem separador carrega as demais; falha
simulada na escrita preserva o arquivo anterior.

#### FIX-21 — Workflow de release

**Resolve:** BUG-23, BUG-19 (CI) · **Status:** concluída na branch `fix/21-release-workflow`

- Disparar apenas em `push: tags: v*`, ou apenas em `release: published`.
- Adicionar um passo `ant test` (suíte offline) antes do empacotamento.
- Verificar SHA-256 de `ivy` e `xz`, e fixar as actions por SHA.
- Criar um workflow de CI separado para PRs, rodando `ant test`.

#### FIX-22 — Limpeza de resíduos

**Resolve:** BUG-24 · **Relaciona:** T50 · **Status:** concluída na branch `fix/22-javafx-residuos`

- Remover `initJavaFX`, `invokeJavaFX`, a mensagem "Please install JavaFX" e
  `UserFiles.FileChooser.JavaFX`, migrando as preferências salvas para `Swing`.
- Remover a restrição de `-clear-cache` sem console.

## 5. Marcos

| Marco | Conteúdo | Critério de saída |
|---|---|---|
| **M-C0** | FIX-00 | `ant test` offline, verde e bloqueante |
| **M-C1** | FIX-01 a FIX-04 | As PoCs de BUG-01, 02, 03 e 07 viram testes verdes; nenhuma operação apaga dados fora do escopo |
| **M-C2** | FIX-05 a FIX-09 | Nenhum código executado a partir do CWD; ADR do sandbox aprovado; endpoints em HTTPS |
| **M-C3** | FIX-10 a FIX-13 | GUI responsiva durante rename/match; cancelamento funcional; ajuda coerente |
| **M-C4** | FIX-14 a FIX-18 | Launcher, `--apply date`, `ReleaseInfo`, `{hdr}` e `--conflict index` corretos e testados |
| **M-C5** | FIX-19 a FIX-22 | CI com testes; suíte sem falhas externas; código morto removido |

Recomenda-se publicar uma versão de correção (**4.9.1**) ao fim de M-C1 e
M-C2, porque esses itens envolvem perda de dados e segurança.

## 6. Definition of Done

Uma FIX só é concluída quando:

- existe um teste offline que falhava antes da correção e passa depois;
- `ant test`, `ant fatjar` e `ant portable` passam;
- o smoke test CLI (`-version`, `-help`, `--action test`) passa com o launcher
  da raiz;
- a documentação e a ajuda afetadas foram atualizadas;
- o BUG correspondente foi marcado como **Corrigido** em
  `auditoria-bugs-falhas.md`, com referência ao commit;
- a mudança não introduz novos avisos `[removal]` nem caminhos relativos ao
  diretório de trabalho.

## 7. Próximo passo

Começar por **FIX-00**, um esforço pequeno que dá a rede de segurança para
validar as demais correções. Logo em seguida vêm **FIX-01** e **FIX-02**, que
evitam perda de dados de usuários que já usam `--apply prune` ou têm um
histórico grande.
