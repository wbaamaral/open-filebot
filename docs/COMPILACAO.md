# Guia de Compilação e Distribuição do FileBot

Este documento detalha o processo completo de configuração do ambiente, resolução de dependências e geração dos pacotes de distribuição do FileBot.

---

## 1. Pré-requisitos do Ambiente

O projeto foi modernizado para executar com **Java 25 LTS** e utiliza o **Apache Ant** com **Apache Ivy** para automação de tarefas e resolução de bibliotecas.

### 1.1 Ferramentas Necessárias
* **Java SDK 25 (LTS)**: Compilador e runtime.
* **Apache Ant 1.10+**: Motor de build baseado em `build.xml`.
* **Apache Ivy 2.5+**: Gerenciador de dependências externas para o Ant.
* **SDKMAN!** (Recomendado): Para gerenciamento de versões de SDKs Java e ferramentas auxiliares.

### 1.2 Configuração via SDKMAN!

Se você utiliza o [SDKMAN!](https://sdkman.io/), certifique-se de carregar o ambiente em seu terminal:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
```

Instale o Java 25 e o Apache Ant (caso ainda não estejam instalados):

```bash
# Instalação do Java 25
sdk install java 25.0.4-oracle  # ou a distribuição OpenJDK 25 de sua preferência

# Instalação do Apache Ant
sdk install ant
```

Verifique se ambos estão ativos na sessão atual:
```bash
java -version
ant -version
```

---

## 2. Configuração de Extensões do Ant (Ivy e XZ)

O Apache Ant precisa de duas bibliotecas complementares em seu diretório de extensões (`~/.ant/lib/`):
1. **Apache Ivy (`ivy.jar`)**: Responsável por resolver e baixar as dependências externas.
2. **XZ for Java (`xz.jar`)**: Responsável por habilitar a compressão `.tar.xz` no empacotamento da distribuição portátil.

Configure ambas com os comandos:

```bash
mkdir -p ~/.ant/lib
# 1. Conector do Apache Ivy
curl -fsSL https://repo1.maven.org/maven2/org/apache/ivy/ivy/2.5.2/ivy-2.5.2.jar -o ~/.ant/lib/ivy.jar
# 2. Biblioteca de compressão XZ
curl -fsSL https://repo1.maven.org/maven2/org/tukaani/xz/1.6/xz-1.6.jar -o ~/.ant/lib/xz.jar
```

*(Nota: o alvo `ant resolve` também copia automaticamente o `xz.jar` baixado para `~/.ant/lib/` caso já tenha sido resolvido).*

---

## 3. Resolução de Dependências (`ant resolve`)

Antes de realizar a primeira compilação, é obrigatório baixar as bibliotecas externas e descompactar os binários nativos das plataformas:

```bash
ant resolve
```

### O que acontece nesta etapa:
1. O Ivy lê o arquivo [ivy.xml](file:///home/wbaamaral/acervo/projetos/filebot/ivy.xml) e baixa todos os `.jar` necessários (como Groovy, JNA, Ehcache, Lanterna, Jsoup, etc.) para a pasta `lib/ivy/`.
2. O Ant executa o alvo `resolve-import-native`, extraindo as bibliotecas dinâmicas nativas (`.so`, `.dll`, `.dylib`) dos pacotes `jna` e `sevenzipjbinding` para o diretório `lib/native/` nas arquiteturas suportadas:
   * Linux: `linux-amd64`, `linux-i686`, `linux-armv7l`, `linux-armv8` (aarch64)
   * Windows: `win32-x64`, `win32-x86`
   * macOS: `mac-x86_64`

> **Atenção:** Se você pular este passo e tentar rodar `ant portable` ou `ant fatjar` direto, o build falhará com:  
> `BUILD FAILED: build.xml:773: .../lib/ivy/jar does not exist.`

---

## 4. Gerando os Pacotes de Distribuição

Com as dependências resolvidas, execute o comando correspondente ao formato de distribuição desejado:

### 4.1 Distribuição Portátil Linux (Recomendada)

Gera um pacote completo e autocontido para Linux, incluindo bibliotecas nativas, launcher com argumentos atualizados para o Java 25 e arquivos compactados de release:

```bash
ant portable
```

**Arquivos gerados em `dist/`:**
* `dist/FileBot_4.8.0-portable/`: Diretório contendo:
  * `FileBot.jar`: JAR executável contendo todas as classes.
  * `filebot` e `filebot.sh`: Scripts executáveis de inicialização já configurados com as opções de `--add-opens` e `--enable-native-access` necessárias para o Java 25.
  * `lib/`: Módulos nativos (`.so`) e binários `fpcalc` organizados por arquitetura (`x86_64`, `aarch64`, `armv7l`, `i686`).
  * `data/`: Diretório local para cache e preferências portáteis.
* `dist/FileBot_4.8.0-portable.tar.xz`: Arquivo compactado em tar.xz para distribuição.
* `dist/FileBot_4.8.0-portable.tar.gz`: Arquivo compactado em tar.gz.

### 4.2 Executável Standalone Fat JAR

Gera um único arquivo `.jar` contendo todas as classes compiladas e bibliotecas mescladas:

```bash
ant fatjar
```

* **Arquivo gerado:** `dist/FileBot_4.8.0.jar`.

### 4.3 Pacotes do Instalador Debian (`.deb`)

Para distribuições Debian/Ubuntu:

```bash
ant deb
```

* **Arquivo gerado:** Pacotes `.deb` em `dist/` organizados por arquitetura.

### 4.4 Outros Formatos Disponíveis

* `ant spk`: Gera pacote de instalação para Synology NAS (DiskStation Manager - DSM).
* `ant msi`: Gera instalador MSI para Windows (requer utilitários Windows como WiX).
* `ant appx`: Gera pacote UWP para Windows 10/11.
* `ant cask`: Gera bundle de aplicativo para macOS.

### 4.5 Testes Automatizados

```bash
# Suíte offline: não usa rede e falha o build se qualquer teste falhar
ant test

# Testes que criam janelas e painéis reais (precisam de display; em servidor/CI use xvfb-run)
xvfb-run -a ant test-gui

# Testes de contrato com serviços externos (TMDb, TheTVDB, AniDB, OpenSubtitles...)
# Não bloqueiam o build: falhas podem vir de mudanças nos serviços ou nos dados
ant test-online
```

* **Suítes:** `net.filebot.OfflineTests` (bloqueante), `net.filebot.GuiTests`
  (bloqueante, precisa de display) e `net.filebot.OnlineTests` (opcional). `net.filebot.AllTests` reúne as duas para execução em IDE.
* **Isolamento:** ambos os alvos rodam em `build/test-sandbox/`, com
  `application.dir`, `java.io.tmpdir`, preferências (`FilePreferencesFactory`)
  e lixeira (`net.filebot.trash.home`) apontando para lá, sempre em modo
  headless. Os testes nunca tocam `~/.filebot`, `/tmp` nem as
  preferências reais do usuário; o diretório é recriado a cada execução.
* **Relatórios:** saída em texto no console e XML em `build/reports/test/`.
* **Novos testes:** testes sem rede entram em `OfflineTests`; testes que dependem
  de APIs ou dados remotos entram em `OnlineTests`. Para arquivos temporários,
  use `org.junit.rules.TemporaryFolder`, que já fica dentro do sandbox.
* **Conferir que a suíte é realmente offline (Linux):** `unshare -rn ant test`.

---

## 5. Execução do FileBot Após o Build

Após compilar com `ant portable`, você pode executar o FileBot imediatamente a partir da raiz do projeto usando o script launcher:

```bash
./filebot
```

Ou diretamente a partir da pasta de distribuição:
```bash
./dist/FileBot_4.8.0-portable/filebot -version
```

Para abrir a interface gráfica:
```bash
./filebot
```

Para executar comandos na CLI (exemplo de ajuda):
```bash
./filebot -help
```

---

## 6. Limpeza do Ambiente de Compilação

Para limpar todos os binários e diretórios gerados (`build/` e `dist/`):

```bash
ant clean
```

---

## 7. Instalação Global no Sistema (Linux / BigLinux)

Para disponibilizar o FileBot globalmente no sistema (`/opt/filebot`), acessível pelo terminal e integrado aos menus gráficos (como no KDE Plasma do BigLinux):

1. **Instalar arquivos em `/opt/filebot` e link simbólico em `/usr/local/bin`:**
   ```bash
   sudo mkdir -p /opt/filebot
   sudo cp -r dist/FileBot_4.8.0-portable/* /opt/filebot/
   sudo chmod 755 /opt/filebot/filebot /opt/filebot/filebot.sh
   sudo find /opt/filebot/lib -type f -exec chmod 755 {} +
   sudo ln -sf /opt/filebot/filebot /usr/local/bin/filebot
   ```

2. **Instalar Ícones e Atalho do Menu (.desktop):**
   ```bash
   # Ícones
   sudo mkdir -p /usr/share/icons/hicolor/scalable/apps /usr/share/pixmaps
   sudo cp installer/icons/filebot.svg /usr/share/icons/hicolor/scalable/apps/filebot.svg
   sudo cp installer/icons/filebot.svg /usr/share/pixmaps/filebot.svg
   sudo cp installer/icons/icon256.png /usr/share/pixmaps/filebot.png

   # Entrada no Menu de Aplicações
   sudo cp /tmp/filebot.desktop /usr/share/applications/filebot.desktop
   sudo update-desktop-database /usr/share/applications
   sudo gtk-update-icon-cache -f -t /usr/share/icons/hicolor
   ```

