# Open FileBot

> **O organizador e renomeador de mídias definitivo, 100% livre, comunitário e modernizado para Java 25.**

Este projeto é a continuação livre e comunitária do código-fonte do **FileBot** (criado originalmente por Reinhard Pointner - [rednoah](https://github.com/rednoah)), preservado e modernizado como software de código aberto (*open-source*) sob a licença GNU AGPLv3.

---

## 🙏 Reconhecimento e Gratidão ao Criador Original

Expressamos nosso mais sincero e profundo agradecimento a **Reinhard Pointner** ([@rednoah](https://github.com/rednoah)), arquiteto e criador do FileBot original.

Foi a sua visão pioneira, dedicação incansável e genialidade técnica ao longo dos anos que construíram a base formidável deste software — desde o motor de correspondência inteligente (*fuzzy matching*), passando pelo poderoso ecossistema de *format bindings* e scripts Groovy, até a integração com serviços mundiais de metadados.

Sem o trabalho pioneiro e a maestria técnica de Reinhard, este projeto e tudo o que desfrutamos hoje simplesmente não existiriam. O **Open FileBot** existe como um tributo à longevidade dessa criação, mantendo esse legado vivo, livre de bloqueios e acessível para toda a comunidade.

---

## 🚀 Estado Atual e Modernização

Este fork foi ativamente atualizado e modernizado com foco em compatibilidade e longevidade:

* **Compatibilidade com Java 25 (LTS)**: Todo o código-fonte, opções de compilação e flags de inicialização da JVM foram adaptados para o Java 25 moderno (`--enable-native-access`, ajustes de reflexão e aberturas modulares `--add-opens`).
* **Desacoplamento JavaFX -> Swing Puro**: A dependência legada do ecossistema JavaFX (como o painel *Getting Started*) foi desacoplada e convertida para componentes Swing nativos de alto desempenho, eliminando dependências externas desnecessárias.
* **Correções de Estabilidade e XML**: Remoção de APIs descontinuadas do JDK (como referências diretas ao JAXB em módulos de histórico) substituídas por parsing padrão DOM.
* **Aparência Consistente**: Fontes Inter e JetBrains Mono embutidas, diálogo de aparência (tema, fonte e tamanho), ícones SVG (Tabler) que acompanham o tema e [design tokens](docs/DESIGN_TOKENS.md) como fonte única de cores, espaçamentos e tipografia.
* **Distribuição Portátil Pronta para Uso**: Inclusão de alvo no Ant para geração de pacote portátil Linux completo com bibliotecas nativas (`fpcalc`, 7-Zip, JNA) e launcher executável [./filebot](file:///home/wbaamaral/acervo/projetos/filebot/filebot) na raiz.

---

## 🛠️ Compilação e Distribuição

O processo de compilação é automatizado via **Apache Ant** e **Apache Ivy**.

Para instruções completas passo a passo sobre como instalar ferramentas via SDKMAN, configurar o Ivy, baixar dependências e compilar os pacotes de distribuição, consulte o guia dedicado:

👉 **[Guia Completo de Compilação e Distribuição (docs/COMPILACAO.md)](file:///home/wbaamaral/acervo/projetos/filebot/docs/COMPILACAO.md)**

### Resumo Rápido dos Comandos:

```bash
# 1. Baixar o conector do Ivy para o Ant (uma única vez)
mkdir -p ~/.ant/lib && curl -fsSL https://repo1.maven.org/maven2/org/apache/ivy/ivy/2.5.2/ivy-2.5.2.jar -o ~/.ant/lib/ivy.jar

# 2. Baixar todas as dependências e binários nativos
ant resolve

# 3. Gerar o pacote de distribuição portátil (Linux)
ant portable

# 4. Executar diretamente
./filebot
```

---

## 📚 Documentação do Projeto

* 📖 **[docs/COMPILACAO.md](file:///home/wbaamaral/acervo/projetos/filebot/docs/COMPILACAO.md)**: Guia passo a passo de configuração do ambiente, resolução de dependências e criação de pacotes (`portable`, `fatjar`, `deb`, `msi`, `spk`).
* 📋 **[docs/FUTURE_FEATURES.md](file:///home/wbaamaral/acervo/projetos/filebot/docs/FUTURE_FEATURES.md)**: Catálogo detalhado das novas APIs (TheTVDB v4, OpenSubtitles REST, TMDb TV) e features modernas mapeadas para implementação futura no projeto.
* 🧭 **[Auditoria e roadmap incremental](docs/plan/auditoria-roadmap-modernizacao.md)**: Estado real das funcionalidades, prioridades, dependências, tarefas e critérios de aceite.

---

## 🌿 Ponto de Origem do Fork

Caso tenha interesse no código exatamente como estava no momento do fork original, consulte o branch `fork-point`.

---

## ⚖️ Licença

O código-fonte do Open FileBot é software livre e está licenciado sob os termos da **GNU Affero General Public License v3.0 (GNU AGPLv3)**. Consulte o arquivo [LICENSE.md](file:///home/wbaamaral/acervo/projetos/filebot/LICENSE.md) para o texto completo da licença.

As fontes embutidas **Inter** (© The Inter Project Authors) e **JetBrains Mono** (© The JetBrains Mono Project Authors) são distribuídas sob a **SIL Open Font License 1.1**, por meio dos pacotes `flatlaf-fonts-inter` e `flatlaf-fonts-jetbrains-mono`; o texto da licença acompanha cada fonte dentro do JAR.

Os ícones de interface são gerados a partir do **[Tabler Icons](https://tabler.io/icons)** (© Paweł Kuna), distribuído sob a **licença MIT**; o texto da licença está em `source/net/filebot/resources/svg/LICENSE-tabler-icons.txt`.
