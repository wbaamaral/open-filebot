# Auditoria de bugs e falhas

**Data:** 2026-10-03

**Versão auditada:** 4.9.0 (`1cb08f1`, branch `docs-auditoria-roadmap-modernizacao`)

**Complementa:** [`auditoria-roadmap-modernizacao.md`](auditoria-roadmap-modernizacao.md),
que cobre lacunas de funcionalidade (AUD-01 a AUD-22). Este documento registra
**defeitos**: comportamento incorreto, perda de dados, falhas de segurança e
regressões. Nenhuma correção foi aplicada; os itens aguardam verificação para
virar plano.

## 1. Método

- Leitura de todo o código alterado pelo fork desde a importação do FileBot 4.8.0
  (`d2ba4b7..HEAD`) e varredura por padrões de risco no restante (~60 mil linhas).
- Compilação com `javac -Xlint:all` em Java 25.0.4: 0 erros, 212 avisos
  (110 `unchecked`, 56 `deprecation`, 45 `removal`).
- `ant test`: **180 testes, 22 falhas, 15 erros, 4 ignorados**, e mesmo assim
  `BUILD SUCCESSFUL` (ver BUG-19). Todas as falhas observadas dependem de
  serviços externos ou de dados remotos que mudaram.
- Provas de conceito (PoC) executadas em diretórios temporários com uma
  **cópia** do pacote portátil (`dist/FileBot_4.9.0-portable`), sem tocar em
  dados reais.

### Legenda

| Campo | Valores |
|---|---|
| Severidade | **Crítica** (perda de dados ou execução de código), **Alta** (falha funcional relevante ou risco de segurança), **Média** (comportamento incorreto com contorno), **Baixa** (qualidade, UX, manutenção) |
| Evidência | **PoC**: reproduzido; **Leitura**: comprovado pelo código ou pela especificação da JDK; **Plausível**: depende de ambiente e ainda precisa de reprodução |
| Origem | **Fork**: introduzido após a importação; **Upstream**: herdado do 4.8.0 |

## 2. Resumo

| ID | Severidade | Origem | Evidência | Defeito |
|---|---|---|---|---|
| BUG-01 | Crítica | Fork | PoC | `--apply prune` apaga diretórios vazios não relacionados, inclusive com `--action test` — **corrigido: FIX-01** |
| BUG-02 | Crítica | Fork | PoC | `history.xml` corrompido é sobrescrito e todo o histórico anterior é perdido — **corrigido: FIX-02** |
| BUG-03 | Alta | Upstream | PoC | Extração com `SevenZipNativeBindings` permite *path traversal* (Zip Slip) — **corrigido: FIX-03** |
| BUG-04 | Alta | Fork | Leitura | Rename e Match na GUI bloqueiam a EDT: sem diálogo de progresso nem cancelamento — **corrigido: FIX-10** |
| BUG-05 | Alta | Fork | Leitura | Verificação de assinatura dos scripts removida, com carga a partir do diretório atual — **corrigido: FIX-05** |
| BUG-06 | Alta | Fork | PoC | Launcher raiz `./filebot` aponta para `FileBot_4.8.0-portable`, que não existe — **corrigido: FIX-14** |
| BUG-07 | Alta | Upstream | PoC | `-revert` falha em modo headless (`HeadlessException`) para copy, hardlink e symlink — **corrigido: FIX-04** |
| BUG-08 | Alta | Fork/JDK | Leitura | Sandbox das expressões de formato está inativo no Java 25 — **corrigido: FIX-06** |
| BUG-09 | Média | Fork | Leitura | `--apply date` em symlink e hardlink altera o arquivo original — **corrigido: FIX-15** |
| BUG-10 | Média | Fork | Leitura | `ProgressMonitor`: cancelar não interrompe o worker e a barra satura acima de 2 GiB — **corrigido: FIX-11** |
| BUG-11 | Média | Fork | Leitura | `-script` com espaço, `;` ou quebra de linha é executado como Groovy inline — **corrigido: FIX-07** |
| BUG-12 | Média | Fork | Leitura | Dados de `ReleaseInfo` nunca são atualizados e ignoram override por `-Durl.*` — **corrigido: FIX-16** |
| BUG-13 | Média | Upstream | PoC (teste) | Índice de títulos AniDB responde `403` via `http://` — **corrigido: FIX-08** |
| BUG-14 | Média | Fork | Leitura | Ajuda e "Atalhos" documentam teclas erradas; `Ctrl+Shift+Delete` limpa o cache — **corrigido: FIX-12** |
| BUG-15 | Média | Upstream | Leitura | `-exec` descarta argumentos que falham e desloca a linha de comando — **corrigido: FIX-09** |
| BUG-16 | Média | Fork | PoC | `{hdr}` nunca reconhece `HDR10+` pelo nome do arquivo — **corrigido: FIX-17** |
| BUG-17 | Média | Upstream | Leitura | Cancelar script no GroovyPad lança `UnsupportedOperationException` — **corrigido: FIX-13** |
| BUG-18 | Média | Upstream/Fork | Leitura | Importação e exportação de histórico na GUI vazam *file handles* e escondem falhas — **corrigido: FIX-02** |
| BUG-19 | Média | Fork | PoC | `ant test` não falha o build e o CI de release não executa testes — **corrigido: FIX-00, FIX-19, FIX-21** |
| BUG-20 | Baixa | Fork | Leitura | Detecção de tema escuro executa processos externos na EDT, sem timeout — **corrigido: FIX-13** |
| BUG-21 | Baixa | Upstream | Leitura | `--conflict index` gera `Nome.1.null` sem extensão e falha após 99 cópias — **corrigido: FIX-18** |
| BUG-22 | Baixa | Upstream | Leitura | Preferências gravadas sem atomicidade e perdidas se o arquivo estiver malformado — **corrigido: FIX-20** |
| BUG-23 | Baixa | Fork | Leitura | Workflow de release dispara em duplicidade e usa dependências sem verificação — **corrigido: FIX-21** |
| BUG-24 | Baixa | Fork | Leitura | Resíduos do JavaFX e mensagens enganosas na inicialização — **corrigido: FIX-22** |
| BUG-25 | Baixa | Upstream | PoC | `DateMetricTest` desatualizado e fora de qualquer suíte — **corrigido: FIX-23** |
| BUG-26 | Alta | Fork | PoC | Trocar o tema em tempo de execução quebra a interface (`ClassCastException`) — **corrigido** |

## 3. Detalhamento

### BUG-01 — `--apply prune` destrói diretórios fora do escopo, inclusive em dry-run

> **Status:** corrigido pela FIX-01 (branch `fix/01-apply-prune`). O
> pós-processamento foi extraído para `PostProcessing`/`PruneEmptyFolders` e
> coberto por 17 testes offline. A PoC original agora preserva
> `keep_empty_dir`, `sub/deeper` e a pasta de entrada; em `--action test`, nada
> é alterado.

**Arquivo:** [`ArgumentProcessor.java:138-174`](../../source/net/filebot/cli/ArgumentProcessor.java)

`applyPostProcessing` é chamado sempre que `cli.rename` devolve destinos, e
`--action test` também devolve. `pruneEmptyDirectories` percorre
**recursivamente** o diretório pai de cada arquivo de origem e apaga **todo**
subdiretório vazio, mesmo que nunca tenha contido mídia processada.

- **PoC:** `in/Avatar.2009.1080p.mkv` + `in/keep_empty_dir/` + `in/sub/deeper/`;
  `filebot -rename in --db TheMovieDB --action test --apply prune`. Resultado:
  o arquivo não foi renomeado, como esperado em teste, mas `keep_empty_dir`,
  `sub/deeper` e `sub` foram **apagados**.
- Também afeta `copy`, `hardlink`, `symlink`, `clone` e `duplicate`, em que a
  origem continua no lugar.
- `child.isDirectory()` segue symlinks: o prune pode entrar em diretórios fora
  da árvore e, com um laço de symlinks, terminar em `StackOverflowError`.
- Se a própria pasta de entrada ficar vazia, ela também é removida.

**Direção:** rodar apenas em `move`/`keeplink` e nunca em `test`; limitar a
remoção às pastas que de fato tiveram arquivos movidos, subindo até a raiz de
entrada; não seguir symlinks. Relaciona-se com T21 do roadmap.

### BUG-02 — Perda silenciosa de todo o histórico de renomeações

> **Status:** corrigido pela FIX-02 (branch `fix/02-historico-atomico`).
> `importHistory` agora lança `HistoryFormatException` sem imprimir no stderr,
> e `exportHistory` propaga falhas. No commit, um arquivo ilegível é preservado
> como `history.xml.corrupt-<data>` com aviso no log. A gravação passou a usar
> `history.xml.tmp` + `ATOMIC_MOVE`, com lock em `history.xml.lock`, e uma
> falha de escrita mantém o arquivo anterior e a sessão para nova tentativa. O
> CLI faz commit antes do `System.exit`, para que o aviso não se perca no
> shutdown hook, e o `-revert` com arquivo corrompido falha com mensagem limpa,
> sem alterar o arquivo. Há 12 testes offline.

**Arquivos:** [`History.java:228-279`](../../source/net/filebot/History.java),
[`HistorySpooler.java:56-90`](../../source/net/filebot/HistorySpooler.java)

`History.importHistory` captura qualquer exceção e devolve um `History`
**vazio**. `HistorySpooler.commit` trata esse retorno como válido, acrescenta a
sessão atual e sobrescreve o arquivo com `truncate`. `exportHistory` também
engole exceções; uma falha no meio da escrita seguida de `truncate` deixa o
arquivo parcial.

- **PoC:** um `history.xml` com 2 entradas antigas e o fechamento `</history>`
  ausente. Após uma renomeação `move`, o parser imprimiu
  `[Fatal Error] ... XML document structures must start and end within the same entity`
  e o arquivo passou a conter **apenas** a nova entrada; as anteriores
  desapareceram.
- Consequência: `-revert` e a GUI de histórico perdem a capacidade de desfazer
  operações antigas.

**Direção:** propagar a falha de leitura; não sobrescrever um arquivo ilegível
(preservar como `history.xml.corrupt-<data>`); gravar em arquivo temporário e
fazer *atomic move*; adicionar teste para XML truncado.

### BUG-03 — *Path traversal* na extração de arquivos compactados (Zip Slip)

> **Status:** corrigido pela FIX-03 (branch `fix/03-zip-slip`).
> `FileMapper.resolve` rejeita entradas absolutas, UNC, com letra de unidade ou
> com `..` que saiam da pasta de saída, e `getStream` também confere o caminho
> real, contra symlinks existentes no destino. As entradas rejeitadas são
> puladas com aviso e não entram na lista de arquivos extraídos devolvida ao
> AMC. Comportamento por extrator:
>
> - `SevenZipNativeBindings` e `SevenZipExecutable` pulam a entrada ilegal e
>   extraem o restante;
> - `ApacheVFS` recusa o arquivo inteiro (`Invalid relative file name`), o que
>   é seguro.
>
> Há 16 testes offline (7 de `FileMapper` e 9 de extração real nos três
> extratores). Contra o código antigo, 6 dos 9 cenários de extração falham.
>
> Achados adicionais corrigidos na mesma tarefa:
>
> - `SevenZipExecutable` com seleção vazia chamava `7z x` sem arquivos, o que
>   extrai **tudo**;
> - nomes de entrada começando com `-` podiam ser interpretados como opções do
>   `7z` (agora há `--`);
> - o classpath de `ant test` incluía um `dist/lib/filebot.jar` antigo,
>   duplicando classes e recursos. Isso fazia o `ApacheVFS` falhar nos testes
>   (`Multiple providers registered for URL scheme "rar"`) e podia rodar testes
>   contra código desatualizado. O pacote portátil não é afetado.

**Arquivo:** [`FileMapper.java:33-48`](../../source/net/filebot/archive/FileMapper.java)

`getOutputFile` faz `new File(outputDir, entry.getPath())` sem normalizar nem
verificar se o resultado continua dentro de `outputDir`. O extrator
`SevenZipNativeBindings` é o **padrão do código**
([`Archive.java:27`](../../source/net/filebot/archive/Archive.java)) quando
`net.filebot.Archive.extractor` não está definido; o script portátil força
`ApacheVFS`, mas JAR, GUI e outros empacotamentos usam o padrão.

- **PoC:** um zip com a entrada `../../ESCAPED.txt`, extraído com
  `filebot -extract evil.zip --output out` e `SevenZipNativeBindings`, criou
  `ESCAPED.txt` **dois níveis acima** da pasta de saída. Com `ApacheVFS`, o
  arquivo não escapou.
- Vetor real: arquivos `.rar`/`.zip` baixados e processados automaticamente
  pelo AMC (`-extract`), o que permite sobrescrever arquivos do usuário.

**Direção:** canonicalizar o destino e rejeitar entradas fora de `outputDir`,
aplicando o mesmo controle a `SevenZipExecutable` e `ApacheVFS`; criar teste com
um arquivo malicioso.

### BUG-04 — GUI congela durante Rename e Match — **Corrigido (FIX-10)**

**Arquivos:** [`RenameAction.java`](../../source/net/filebot/ui/rename/RenameAction.java),
[`MatchAction.java`](../../source/net/filebot/ui/rename/MatchAction.java),
[`ProgressMonitor.java`](../../source/net/filebot/util/ui/ProgressMonitor.java)

`actionPerformed` rodava na EDT e chamava `ProgressMonitor.runTask(...).get()`,
bloqueando a EDT até o fim do trabalho. O diálogo de progresso e o botão
**Cancel** nunca apareciam.

**Correção:** `ProgressMonitor.runTask` agora aceita um callback `onComplete`
executado na EDT ao final da tarefa. `RenameAction` e `MatchAction` usam o
callback em vez de `.get()` e desabilitam as ações durante a execução.

### BUG-05 — Scripts executados sem verificação de integridade, também a partir do diretório atual

> **Status:** corrigido pela FIX-05 (branch `fix/05-integridade-scripts`). O
> pacote só é usado se o SHA-256 do jar descompactado conferir com
> `script.bundle.sha256` (em `app.properties`). As origens, em ordem, são o
> recurso embutido, `/opt/filebot/scripts` e o download de `github.stable`;
> todas são verificadas, e uma origem inválida é ignorada com aviso. Os
> caminhos relativos ao diretório de trabalho foram removidos. Sem nenhuma
> origem válida, a execução é recusada (`ScriptIntegrityException`). O
> certificado `repository.cer`, que não era usado, foi removido. A PoC
> (pacote embutido adulterado e pasta de trabalho com
> `downloads/scripts/m1.jar.xz` malicioso) não executa o script malicioso.
> Há 7 testes offline.

**Arquivos:** [`ScriptBundle.java:32-51`](../../source/net/filebot/cli/ScriptBundle.java),
[`ScriptSource.java:29-69`](../../source/net/filebot/cli/ScriptSource.java)

O fork removeu a verificação de certificado do `m1.jar.xz`, abrindo o
`JarInputStream` com `verify=false` e sem checar `getCertificates()`. Em
seguida, `fn:<script>` procura o pacote em:

1. recurso empacotado no JAR;
2. `downloads/scripts/m1.jar.xz` e `../downloads/scripts/m1.jar.xz`,
   **relativos ao diretório de trabalho**;
3. `/opt/filebot/scripts/m1.jar.xz`;
4. download de `github.stable`, com cache de uma semana.

Hoje o item 1 existe no JAR oficial e mascara o problema. Em builds sem o
recurso, ou se ele estiver corrompido, rodar `filebot -script fn:amc` dentro de
uma pasta controlada por terceiros (por exemplo, uma pasta de downloads)
executa Groovy arbitrário. O download remoto também não tem checksum nem
assinatura. O certificado `repository.cer` continua sendo carregado, mas não é
usado.

**Direção:** eliminar caminhos relativos ao CWD; validar hash ou assinatura
própria do pacote (T33 do roadmap); falhar fechado.

### BUG-06 — Launcher da raiz quebrado — **Corrigido (FIX-14)**

**Arquivo:** [`filebot`](../../filebot)

O launcher apontava para `dist/FileBot_4.8.0-portable/filebot`, mas o build
gera `dist/FileBot_4.9.0-portable`.

**Correção:** launcher resolve dinamicamente: usa `dist/portable` (symlink
estável gerado por `ant portable`) ou o diretório `FileBot_*-portable` mais
recente. Erro claro se nenhum build existir.

### BUG-07 — `-revert` falha em servidores headless

> **Status:** corrigido pela FIX-04 (branch `fix/04-lixeira-revert-headless`).
> `UserFiles.trash` só usa `java.awt.Desktop` fora do modo headless e, no
> Linux e em outros Unix, usa a lixeira freedesktop.org (`XdgTrash`):
> `$XDG_DATA_HOME/Trash` para arquivos na mesma partição da home e
> `<partição>/.Trash-<uid>` para outras partições, sem copiar arquivos
> grandes, e com `.trashinfo` para que o gerenciador de arquivos possa
> restaurar. A exclusão permanente só acontece sem nenhuma lixeira disponível
> e agora gera aviso no log. A PoC original reverte a cópia e move o arquivo
> para a lixeira. São 9 testes offline; contra o código antigo, 3 dos 4
> cenários de revert falham com `HeadlessException`.

**Arquivos:** [`UserFiles.java:33-50`](../../source/net/filebot/UserFiles.java),
[`StandardRenameAction.java:194-233`](../../source/net/filebot/StandardRenameAction.java)

`revert` chama `UserFiles.trash`, que usa `Desktop.getDesktop()` sem verificar
`GraphicsEnvironment.isHeadless()`. O script portátil ativa headless quando não
há `DISPLAY`/`WAYLAND_DISPLAY`, que é o cenário típico de NAS, servidor e cron.

- **PoC:** depois de `--action copy`, `filebot -revert <pasta>` sem display
  gerou `Failed to revert file: java.awt.HeadlessException`; a cópia
  permaneceu.
- No Linux com GUI, o AWT normalmente não suporta `MOVE_TO_TRASH`. Nesse caso,
  `trash` vira **exclusão permanente e recursiva** (`FileUtilities.delete`),
  sem aviso. Isso ainda precisa ser confirmado por *desktop environment*.

**Direção:** checar headless e suporte antes; implementar a lixeira XDG
(`~/.local/share/Trash`) no Linux; registrar a exclusão permanente quando não
houver lixeira. Relaciona-se com T42.

### BUG-08 — Sandbox de expressões de formato inativo no Java 25 — **Corrigido (FIX-06)**

**Arquivos:** [`Main.java`](../../source/net/filebot/Main.java),
[`ExpressionSandbox.java`](../../source/net/filebot/format/ExpressionSandbox.java),
[`SandboxedCompiledScript.java`](../../source/net/filebot/format/SandboxedCompiledScript.java)

Com a JEP 486, o `SecurityManager` foi desativado de forma permanente. O fork
acrescentou um *bypass* para `getSecurityManager() == null`, então expressões
`{...}` eram avaliadas com **privilégios totais**: acesso a arquivos, rede e
processos.

**Correção:** ADR 0001 aceito (opção C). Sandbox em tempo de execução com
`groovy-sandbox` (`ExpressionSandbox`) + limite de tempo (`@TimedInterrupt`).
`Policy`/`SecurityManager`/`AccessController` removidos; `SecureCompiledScript`
e `PrivilegedInvocation` substituídos por `SandboxedCompiledScript`.

### BUG-09 — `--apply date` modifica o original em links — **Corrigido (FIX-15)**

**Arquivo:** [`PostProcessing.java`](../../source/net/filebot/cli/PostProcessing.java)

`File.setLastModified` seguia symlinks e, em hardlinks, alterava o mesmo
inode. Com `--action symlink|hardlink`, o mtime da **mídia original** era
sobrescrito. A data usada era a hora atual (AUD-14).

**Correção:** symlink/hardlink não alteram mtime. Data de lançamento/exibição
propagada do `renameAll` (Movie → ano, Episode → airdate). Data desconhecida
pula com log. `Files.setLastModifiedTime` com `NOFOLLOW_LINKS`.

### BUG-10 — `ProgressMonitor`: cancelamento ineficaz e progresso incorreto — **Corrigido (FIX-11)**

**Arquivo:** [`ProgressMonitor.java`](../../source/net/filebot/util/ui/ProgressMonitor.java)

- `SwingProgressTask` sobrescrevia `run()` sem chamar `FutureTask.run()`, então
  `cancel(true)` **não interrompia** a thread. Um worker bloqueado em I/O
  continuava rodando.
- Depois do cancelamento, havia corrida entre o worker escrevendo em
  `renameLog` e a EDT lendo-o.
- `updateProgress` cortava em `Integer.MAX_VALUE`: com bytes acima de 2 GiB,
  a barra mostrava 100% muito antes do fim.

**Correção:** thread guardada e interrompida em `cancel(true)`. Worker devolve
seu próprio `renameLog` (sem compartilhamento com a EDT). Progresso normalizado
para escala 0–1000 com `long`.

### BUG-11 — Heurística de Groovy inline em `-script` — **Corrigido (FIX-07)**

**Arquivo:** [`ScriptSource.java`](../../source/net/filebot/cli/ScriptSource.java)

`INLINE_GROOVY` vinha antes de `REMOTE_URL` e `LOCAL_FILE` e aceitava qualquer
entrada com espaço, `;` ou `\n` que não seja um arquivo existente. Um caminho
digitado errado (`-script "/media/My Scripts/sort.groovy"`) ou uma URL com
espaço era compilado como código, gerando um erro confuso.

**Correção:** `INLINE_GROOVY` aceita apenas o prefixo explícito `g:`.
Caminhos com espaços e código sem prefixo geram `CmdlineException`.

### BUG-12 — Dados de detecção nunca atualizam e o override `-Durl.*` é ignorado — **Corrigido (FIX-16)**

**Arquivo:** [`ReleaseInfo.java`](../../source/net/filebot/media/ReleaseInfo.java)

A ordem de busca colocava o **recurso empacotado em primeiro lugar**, então
índices ficavam congelados na versão do build. Caminhos relativos ao CWD
permitiam injeção de dados. Índice vazio era silencioso.

**Correção:** nova ordem: override `-Durl.*` → dados locais (AppData/data,
application.dir/data, /opt/filebot/data) → recurso embutido como último
fallback. Caminhos relativos ao CWD removidos. Índice vazio gera
`log.warning`.

### BUG-13 — AniDB: download do índice de títulos devolve `403` — **Corrigido (FIX-08)**

**Arquivo:** [`AnidbClient.java`](../../source/net/filebot/web/AnidbClient.java)

`http://anidb.net/api/anime-titles.dat.gz` respondia `403`. A API HTTP usava
`http://api.anidb.net:9001`. AcoustID, OMDb e TVMaze também usavam `http://`.

**Correção:** endpoints migrados para `https://` em AniDB, AcoustID, OMDb e
TVMaze. Fallback local (`downloads/data/anidb.txt.xz`) disponível via
`AnidbClientWithLocalSearch` / `ReleaseInfo.getAnidbIndex()`.

### BUG-14 — Ajuda descreve atalhos que fazem outra coisa — **Corrigido (FIX-12)**

**Arquivos:** [`FileBotMenuBar.java`](../../source/net/filebot/ui/FileBotMenuBar.java),
[`GettingStartedStage.java`](../../source/net/filebot/ui/GettingStartedStage.java),
[`MainFrame.java`](../../source/net/filebot/ui/MainFrame.java)

| Documentado (antes) | Comportamento real | Correção |
|---|---|---|
| `F5`: executar renomeação | Abre o **GroovyPad** | Documentado como "Abrir GroovyPad" |
| `Ctrl+Shift+Delete`: limpar toda a lista | **Limpa todo o cache** | Documentado como "Limpar cache" + confirmação |
| `F1`: ajuda | Correto | Sem alteração |

### BUG-15 — `-exec` descarta argumentos que falham na avaliação — **Corrigido (FIX-09)**

**Arquivo:** [`ExecCommand.java`](../../source/net/filebot/cli/ExecCommand.java)

Quando um template como `{missing}` lançava exceção, `getArgumentValue` devolvia
`null` e o argumento era **removido** (`filter(Objects::nonNull)`), deslocando as
posições. Por exemplo, `-exec cp {f} {plex.dir}` virava `cp <arquivo>`.

**Correção:** falha fechada — se qualquer argumento do template falhar, o
comando inteiro é abortado (não executado). No modo sequencial, uma falha
em um comando não interrompe os comandos seguintes (cada comando é
independente por arquivo). Aviso com a expressão e o motivo é registrado.

### BUG-16 — `{hdr}` não reconhece `HDR10+` pelo nome do arquivo — **Corrigido (FIX-17)**

**Arquivo:** [`MediaBindingBean.java`](../../source/net/filebot/format/MediaBindingBean.java)

A regex `\b(HDR10\+|HDR10Plus)\b` exigia fronteira de palavra depois do `+`.
`\bDV\b` gerava falsos positivos (DV-rip). HLG não era detectado via
`transfer_characteristics`.

**Correção:** delimitadores explícitos `(?<![A-Za-z0-9])` e `(?![A-Za-z0-9])`.
`DV` restrito a contextos de vídeo (`DV.HDR`, `DoVi`, `DV.HEVC`). HLG
detectado também via `transfer_characteristics`.

### BUG-17 — Cancelar script no GroovyPad quebra — **Corrigido (FIX-13)**

**Arquivo:** [`GroovyPad.java`](../../source/net/filebot/cli/GroovyPad.java)

`Thread.stop()` lança `UnsupportedOperationException` desde o Java 20.

**Correção:** cancelamento por interrupção (`interrupt()`). O script engine
Groovy usa `@ThreadInterrupt` para scripts que ignoram interrupção.

### BUG-18 — Import/export de histórico na GUI

> **Status:** corrigido junto com a FIX-02: streams com try-with-resources, e
> falhas de importação e exportação agora chegam ao log da GUI.

**Arquivo:** [`HistoryDialog.java:604, 634`](../../source/net/filebot/ui/rename/HistoryDialog.java)

`new FileInputStream(file)` e `new FileOutputStream(file)` nunca são fechados.
No Windows, isso mantém o arquivo travado. Como `exportHistory` e
`importHistory` engolem exceções (BUG-02), uma exportação que falha parece bem
sucedida e um arquivo inválido é "importado" como vazio, sem mensagem.

### BUG-19 — Testes não protegem o build — **Corrigido (FIX-00, FIX-19, FIX-21)**

**Status:** corrigido.

- **FIX-00:** `ant test` roda só a suíte offline (`OfflineTests`, 288 testes),
  isolada em `build/test-sandbox/`, e falha o build. Testes de rede vão para
  `ant test-online`, não bloqueante. Sandbox corrige `Preferences.userRoot()`.
- **FIX-21:** `ant test` roda antes do empacotamento no CI de release;
  workflow `ci.yml` separado para PRs.
- **FIX-19:** fixtures offline para TMDb, TVMaze, OMDb, AniDB e AcoustID;
  `@Ignore` documentado em TheTVDB legado e OpenSubtitles XML-RPC (pendente
  T11–T12).

**Arquivos:** [`build.xml`](../../build.xml),
[`.github/workflows/release-build.yml`](../../.github/workflows/release-build.yml),
[`test/resources/web/`](../resources/web/)

Reforça T00 do roadmap.

### BUG-20 — Detecção de tema bloqueia a inicialização — **Corrigido (FIX-13)**

**Arquivo:** [`SwingUI.java`](../../source/net/filebot/util/ui/SwingUI.java)

`isSystemInDarkMode` executava `gsettings` e `dbus-send` **na EDT**, sem
timeout, sem `waitFor` e sem consumir o `stderr`. `'default'` do GNOME forçava
tema claro sem consultar o portal.

**Correção:** timeout de 2s por processo, resultado cacheado, portal consultado
quando GNOME responde `'default'`. Detecção não bloqueia a EDT.

### BUG-21 — `--conflict index` gera nomes inválidos — **Corrigido (FIX-18)**

**Arquivo:** [`CmdlineOperations.java`](../../source/net/filebot/cli/CmdlineOperations.java)

Para pastas ou arquivos sem extensão, `getExtension` devolvia `null` e o nome
gerado ficava `Nome.1.null`. Depois de 99 conflitos, `findFirst().get()`
lançava `NoSuchElementException`.

**Correção:** extensão `null` gera `Nome.1`, `Nome.2`, … (sem `.null`). Limite
fixo de 99 removido (até 10000 com `CmdlineException` legível).

### BUG-22 — Preferências sem gravação atômica — **Corrigido (FIX-20)**

**Arquivo:** [`PropertyFileBackingStore.java`](../../source/net/filebot/util/prefs/PropertyFileBackingStore.java)

`flush` reescrevia o arquivo no mesmo lugar. Uma queda no meio deixava
`prefs.properties` truncado. `sync` usava `substring(0, lastIndexOf(...))` e
uma chave sem separador lançava exceção.

**Correção:** `flush` grava em arquivo temporário e aplica `ATOMIC_MOVE`.
`sync` ignora e registra chaves malformadas. Se a carga falhar, `flush` não
sobrescreve o arquivo.

### BUG-23 — Workflow de release — **Corrigido (FIX-21)**

**Arquivo:** [`release-build.yml`](../../.github/workflows/release-build.yml)

`release: types: [published, created]` mais `push: tags` fazia a mesma release
disparar duas ou três execuções concorrentes. Downloads sem checksum.

**Correção:** trigger apenas em `push: tags: v*`. SHA-256 verificado para
`ivy` e `xz`. `ant test` antes do empacotamento. Workflow de CI separado
para PRs.

### BUG-24 — Resíduos do JavaFX e mensagens enganosas — **Corrigido (FIX-22)**

- `initJavaFX()`/`invokeJavaFX()` removidos de `SwingUI` e `Main`.
- Mensagem "Please install JavaFX" removida.
- `UserFiles.FileChooser.JavaFX` removido (delegava para Swing).
- Restrição de `-clear-cache` sem console removida.

### BUG-25 — `DateMetricTest` desatualizado — **Corrigido (FIX-23)**

**Arquivo:** [`DateMetricTest.java`](../../test/net/filebot/similarity/DateMetricTest.java)

O teste esperava `0` para datas diferentes, mas `DateMetric` devolve `-1`
de propósito (penalidade usada pelo matcher).

**Correção:** teste atualizado para esperar `-1`; separado em casos claros
(matching, non-matching, unknown); incluído em `OfflineTests`.

### BUG-26 — Trocar o tema em tempo de execução quebra componentes com UI própria

**Encontrado em:** teste manual do pacote portátil (diálogo Aparência).

Ao trocar o tema, `SwingUtilities.updateComponentTreeUI` substitui o *UI
delegate* de todos os componentes. Três componentes instalavam uma UI própria
só no construtor e a perdiam:

| Componente | Efeito |
|---|---|
| `SelectButtonTextField` (busca em Episodes e Subtitles) | `ClassCastException` em cascata, porque `getText()` fazia cast fixo para a UI própria; além disso, o texto digitado era apagado |
| `ChecksumTable` (SFV) | Perda silenciosa do arrastar-e-soltar de linhas |
| `SimpleComboBox` (legendas) | Perda do visual próprio |

> **Status:** corrigido na branch `fix/ui-troca-de-tema`. Os três componentes
> reinstalam a própria UI em `updateUI()`, a busca preserva o texto e não faz
> mais cast da UI. `ThemeSwitchTest` (offline) e `PanelThemeSwitchTest` (cria
> todos os painéis e troca tema, fonte e tamanho; roda com
> `xvfb-run ant test-gui`) cobrem o caso. Contra o código antigo, o teste
> reproduz exatamente o `ClassCastException` relatado.

## 4. Falhas da suíte de testes (2026-10-03)

Todas relacionadas a serviços externos. Nenhuma falha em testes puramente
locais.

| Grupo | Casos | Sintoma |
|---|---|---|
| AniDB | `getAnimeTitles`, `search*`, `getEpisodeList*` | HTTP 403 no índice de títulos (BUG-13) |
| TheTVDB (legado) | `search`, `getSeriesInfo`, `getEpisode*`, `getActors`, `getArtwork`, `getLanguages` | API v1/v2 descontinuada (AUD-01) |
| OpenSubtitles XML-RPC | `getSubtitleList*`, `checkMovieHash*`, `fetchSubtitle`, `getIMDBMovieDetails*` | `The response could not be parsed` (AUD-02) |
| TMDb / TVMaze / fanart.tv / OMDb | `getMovieInfo*`, `discover*`, `getAlternativeTitles`, `getPeople`, `getArtwork` | Dados vivos mudaram (títulos, ratings, resolução, contagens) |
| AcoustID | `lookup` | Resposta nula |

## 5. Sugestão de priorização para o plano

1. **Imediato, proteção de dados:** BUG-01, BUG-02, BUG-03, BUG-07.
2. **Segurança e integridade:** BUG-05, BUG-08, BUG-11, BUG-15, BUG-13 (HTTPS).
3. **Regressões da GUI:** BUG-04, BUG-10, BUG-17, BUG-14, BUG-20.
4. **Correção funcional:** BUG-06, BUG-09, BUG-12, BUG-16, BUG-21.
5. **Base de qualidade:** BUG-19 (pré-requisito para validar todos os
   anteriores), BUG-18, BUG-22, BUG-23, BUG-24. **FIX-21 adicionou workflow
   de CI. FIX-19 adicionou fixtures de parsing para TMDb, TVMaze e OMDb.**

Cada correção deve chegar com um teste que reproduza a PoC descrita aqui,
seguindo a Definition of Done do roadmap.

## Anexo A — Serviços externos e falhas da suíte de testes

Os testes de `WebTestSuite` fazem chamadas reais pela internet. Eles quebram
quando o serviço muda dados, troca a API ou bloqueia o acesso, mesmo sem defeito
no código do FileBot.

### A.1 Serviços cobertos pela suíte

| # | Serviço | Uso no FileBot | Resultado em 2026-10-03 |
|---|---|---|---|
| 1 | **AniDB** (`anidb.net`) | Animes: títulos (inclusive japonês e romaji), episódios e numeração absoluta. | `getAnimeTitles`, `search`, `searchNoMatch`, `searchTitleAlias`, `getEpisodeListAll` e `getEpisodeListEncoding` falharam. O índice `http://anidb.net/api/anime-titles.dat.gz` devolve **HTTP 403**. É defeito real (BUG-13), porque a busca de animes depende desse índice. |
| 2 | **TheTVDB** (`thetvdb.com`) | Fonte principal de séries: séries, temporadas, episódios, atores, artes e idiomas. | `search`, `searchGerman`, `getSeriesInfo`, `getEpisodeInfo`, `getEpisodeList*`, `getActors`, `getArtwork` e `getLanguages` falharam. O código usa a API legada, não a v4 (AUD-01), e os dados retornados divergem do esperado: a lista de idiomas passou de 23 para mais de 180, e a arte mudou de 1280x720 para 1920x1080. |
| 3 | **TheMovieDB / TMDb** (`themoviedb.org`) | Fonte principal de filmes (título, ano, aliases, elenco, artes e classificação) e de séries via `TMDbTVClient`. | `getMovieInfo*`, `searchByName*`, `getAlternativeTitles`, `getPeople`, `getArtwork` e `discover*` falharam. A API funciona, mas o conteúdo mudou. Exemplos: "melhor de 2015" deixou de ser *Mad Max: Fury Road* e passou a ser *Avengers: Age of Ultron*; personagens agora vêm com o nome completo. |
| 4 | **TVMaze** (`tvmaze.com`) | Fonte alternativa de séries, com episódios e datas de exibição. | `search` e `getEpisodeListAll` divergiram. A chamada usa `http://` em claro. |
| 5 | **OMDb** (`omdbapi.com`) | Dados complementares de filmes vindos do IMDb, como ratings e descrições. | `getMovieDescriptor2` e `searchMovie3` divergiram. A API key trafega em `http://` em claro. |
| 6 | **OpenSubtitles** (`opensubtitles.org`) | Busca de legendas por nome e por hash do vídeo, e download. | Todos falharam (`getSubtitleList*`, `checkMovieHash*`, `fetchSubtitle`, `getIMDBMovieDetails*`) com `The response could not be parsed`. A API XML-RPC foi descontinuada e a atual é REST (AUD-02). |
| 7 | **AcoustID** (`acoustid.org`) | Identificação de músicas por impressão digital de áudio, gerada pelo `fpcalc`. | `lookup` recebeu resposta nula e o teste quebrou com `NullPointerException`. Usa `http://` com a API key na URL. |

### A.2 Classificação das falhas

- **API descontinuada:** TheTVDB legado e OpenSubtitles XML-RPC. São defeitos
  reais, já previstos no roadmap (T11–T15).
- **Acesso bloqueado:** AniDB (403). É defeito real (BUG-13).
- **Dados vivos mudaram:** TMDb, TVMaze, OMDb e parte do TheTVDB. O código
  funciona; os testes comparam com valores fixos antigos. Devem migrar para
  *fixtures* locais, com testes de contrato opcionais (BUG-19, T00).

### A.3 Serviços usados pelo app e não cobertos pela suíte

- **fanart.tv:** artes de alta qualidade, como logos, banners e clearart.
- **Shooter (`shooter.cn`):** legendas em chinês. O serviço provavelmente está
  descontinuado.
- **GitHub raw (`raw.githubusercontent.com/wbaamaral/filebot-data`):** índices
  de dados e pacote de scripts. Hoje esse download quase nunca é usado, porque
  o recurso embutido no JAR tem precedência (BUG-12).

As chaves desses serviços estão em `app.properties`. A externalização está
prevista em T01 do roadmap.
