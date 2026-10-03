# 🎬 Open FileBot

> **O organizador e renomeador de mídias definitivo, 100% livre, comunitário e modernizado para Java 25.**

[![Java 25](https://img.shields.io/badge/Java-25%20LTS-orange.svg)](https://www.oracle.com/java/)
[![GitHub Release](https://img.shields.io/github/v/release/wbaamaral/open-filebot?color=blue)](https://github.com/wbaamaral/open-filebot/releases)
[![Build Status](https://img.shields.io/github/actions/workflow/status/wbaamaral/open-filebot/release-build.yml?branch=main)](https://github.com/wbaamaral/open-filebot/actions)
[![License](https://img.shields.io/badge/license-AGPL--3.0-blue.svg)](https://github.com/wbaamaral/open-filebot/blob/main/LICENSE.md)

O **Open FileBot** é a versão comunitária e continuada do renomado FileBot, mantendo o software livre de rastreamento, livre de assinaturas pagas e sem travas artificiais de uso.

---

## 🙏 Reconhecimento e Gratidão ao Criador Original

Nosso mais sincero agradecimento a **Reinhard Pointner** ([@rednoah](https://github.com/rednoah)), arquiteto e criador do FileBot original. Foi sua genialidade pioneira, visão e dedicação técnica ao longo dos anos que nos trouxe até aqui e tornou possível a existência de uma das ferramentas de organização de mídia mais poderosas do mundo. O **Open FileBot** existe para honrar esse legado, mantendo-o vivo, livre e aberto para toda a comunidade.

---

## ✨ Principais Destaques e Melhorias

* 🚀 **Modernizado para Java 25 (LTS)**: Compatibilidade completa com o runtime Java moderno, suporte nativo modular (`--enable-native-access` e `--add-opens`), garantindo estabilidade e alta performance.
* 🛡️ **Zero Telemetria e Nagware**: Remoção total de avisos de compra, checagens de licença e redirecionamentos para servidores proprietários.
* 📦 **100% Offline-First**: O aplicativo vem com índices de séries, filmes e animes embutidos no `.jar`. Nunca trava ou dá timeout por falta de internet.
* 🌐 **Ecossistema Aberto via GitHub**: Integração direta com o repositório de dados aberto [`wbaamaral/filebot-data`](https://github.com/wbaamaral/filebot-data), atualizado diariamente por GitHub Actions.
* 🏷️ **Novos Format Bindings**:
  * `{kodi}`: Formatação nativa pronta para bibliotecas Kodi.
  * `{jellyfin}` / `{emby}`: Formatação otimizada para servidores Jellyfin e Emby.
  * `{hdr}`: Detecção automática de formatos HDR (Dolby Vision, HDR10+, HDR10, HLG).
  * `{edition}`: Reconhecimento de edições especiais (*Director's Cut, Extended, Remastered, IMAX, etc.*).
* ⚙️ **Novos Recursos de Linha de Comando (CLI)**:
  * `--apply prune`: Após `--action move`, remove as pastas de origem que ficaram vazias (ou só com `Thumbs.db`/`.DS_Store`), subindo até a pasta de entrada, que é preservada. Não roda com `--action test` nem com ações que mantêm o original (copy, hardlink, symlink, clone, duplicate).
  * `--apply date`: Sincronização automática da data do arquivo com a data de exibição da mídia.
  * `--action duplicate`: Criação rápida de cópias mantendo os arquivos originais intactos.
* 🎨 **Aparência configurável e consistente**: Menu **Aparência → Configurar aparência…** com tema (Automático, Claro, Escuro, Clássico), fonte e tamanho, com pré-visualização e aplicação imediata. Por padrão, usa as fontes embutidas **Inter** (interface) e **JetBrains Mono** (expressões de formato), para ter a mesma aparência em Linux, Windows e macOS; a fonte do sistema continua disponível como opção. Ícones SVG nítidos em qualquer escala e cores, espaçamentos e tipografia definidos por [design tokens](DESIGN_TOKENS.md).
* 🐧 **Suporte Nativo a Linux Moderno**: MediaInfo nativo, Chromaprint (`fpcalc`), suporte a arquivos compactados via Apache Commons VFS e descompactação 7-Zip.

---

## 📥 Como Baixar

Acesse a página de **[Lançamentos / Releases no GitHub](https://github.com/wbaamaral/open-filebot/releases)** e baixe o pacote de sua preferência:

* **Pacote Portátil Linux (`FileBot_4.9.1-portable.tar.xz`)**: Descompacte e execute diretamente com `./filebot`. Não requer instalação.
* **Arquivo Executável Java (`FileBot_4.9.1.jar`)**: Execute em qualquer sistema com Java 25 instalado:
  ```bash
  java -jar FileBot_4.9.1.jar
  ```

---

## 💻 Como Usar

### Interface Gráfica (GUI)
Basta abrir o aplicativo para usar a interface visual tradicional com arrastar e soltar (Drag and Drop):
```bash
filebot
```

### Linha de Comando (CLI)
Exemplo de renomeação automática de episódios:
```bash
filebot -rename "/caminho/dos/downloads" --db TheTVDB -non-strict --format "{n} - {s00e00} - {t}" --apply prune,date
```

Exemplo de verificação de sistema e bibliotecas:
```bash
filebot -script fn:sysinfo
```

---

## 📖 Documentação do Projeto

* [Guia Completo de Compilação Local](COMPILACAO.md)
* [Planejamento de Novas Funcionalidades](FUTURE_FEATURES.md)
* [Auditoria e Roadmap Incremental](plan/auditoria-roadmap-modernizacao.md)
* [Repositório de Dados e Scripts Independentes](https://github.com/wbaamaral/filebot-data)

---

## ⚖️ Licença

O código-fonte do Open FileBot é software livre sob os termos da **GNU Affero General Public License v3.0 (GNU AGPLv3)** disponível em [LICENSE.md](https://github.com/wbaamaral/open-filebot/blob/main/LICENSE.md).
