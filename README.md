# FileBot

Este projeto é um fork do código-fonte oficial do **FileBot** a partir do marco de **23 de março de 2018** para a versão **4.8.0**, preservado e modernizado como software de código aberto (*open-source*).

O repositório original foi retirado do ar após o mantenedor original ([rednoah](https://github.com/rednoah)) fechar o código-fonte para comercializar o software sob um modelo proprietário com licenças pagas, após anos de apoio e colaboração da comunidade aberta.

O mantenedor original realizou ações que prejudicaram a comunidade:
* Adicionou *nagware* (avisos invasivos de compra) ao software original para forçar vendas;
* Tornou propositalmente mais difícil compilar o software a partir do código-fonte;
* Censurou e removeu postagens de usuários nos fóruns oficiais sob sua moderação;
* Enganou a comunidade que apoiou e divulgou o projeto ao longo dos anos;
* Por fim, removeu o repositório público de código aberto do GitHub sob a justificativa de que "não havia outros colaboradores".

Este repositório existe para manter o FileBot verdadeiramente livre, acessível e sob evolução contínua da comunidade.

---

## 🚀 Estado Atual e Modernização

Este fork foi ativamente atualizado e modernizado com foco em compatibilidade e longevidade:

* **Compatibilidade com Java 25 (LTS)**: Todo o código-fonte, opções de compilação e flags de inicialização da JVM foram adaptados para o Java 25 moderno (`--enable-native-access`, ajustes de reflexão e aberturas modulares `--add-opens`).
* **Desacoplamento JavaFX -> Swing Puro**: A dependência legada do ecossistema JavaFX (como o painel *Getting Started*) foi desacoplada e convertida para componentes Swing nativos de alto desempenho, eliminando dependências externas desnecessárias.
* **Correções de Estabilidade e XML**: Remoção de APIs descontinuadas do JDK (como referências diretas ao JAXB em módulos de histórico) substituídas por parsing padrão DOM.
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

---

## 🌿 Ponto de Origem do Fork

Caso tenha interesse no código exatamente como estava no momento do fork original, consulte o branch `fork-point`.

---

## ⚖️ Licença

O código-fonte do Open FileBot é software livre e está licenciado sob os termos da **GNU Affero General Public License v3.0 (GNU AGPLv3)**. Consulte o arquivo [LICENSE.md](file:///home/wbaamaral/acervo/projetos/filebot/LICENSE.md) para o texto completo da licença.
