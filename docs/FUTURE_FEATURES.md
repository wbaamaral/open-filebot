# Catálogo de Features das Versões Recentes do FileBot (Para Implementação Futura)

Este documento registra detalhadamente todas as funcionalidades, melhorias arquiteturais, atualizações de APIs e novos recursos introduzidos nas versões recentes do FileBot (linhas 4.8.5, 4.9.x e 5.x) que foram lançadas sob modelo comercial/pago após o fechamento do repositório original.

Este inventário serve como especificação e guia de implementação para as próximas iterações desta versão open-source.

---

## 1. Integrações com Provedores de Dados e Legendas (Web Services)

### 1.1 TheTVDB API v4 (REST)
* **Contexto**: A API original (v1/v2/v3 baseada em XML e JSON legados) foi permanentemente depreciada e descontinuada pelo TheTVDB.
* **Necessidade**:
  * Implementar suporte ao TheTVDB API v4 via chamadas REST JSON.
  * Suporte a autenticação moderna via Bearer Token JWT (POST `/login` com `apikey` e `user_key`/`pin`).
  * Atualização dos parsers de episódios, temporadas oficiais e rotas de traduções localizadas.

### 1.2 OpenSubtitles REST API (api.opensubtitles.com)
* **Contexto**: O serviço clássico XML-RPC (`api.opensubtitles.org/xml-rpc`) foi oficialmente descontinuado pelo OpenSubtitles no início de 2024.
* **Necessidade**:
  * Substituir o cliente XML-RPC ([`OpenSubtitlesXmlRpc.java`](file:///home/wbaamaral/acervo/projetos/filebot/source/net/filebot/web/OpenSubtitlesXmlRpc.java)) pela nova REST API (`https://api.opensubtitles.com/api/v1/`).
  * Autenticação via `Api-Key` no cabeçalho HTTP e `User-Agent` registrado.
  * Suporte a download de legendas em formato `.srt` e `.vtt` com validação de hash e cotas por usuário (login OAuth2/Bearer).

### 1.3 TheMovieDB (TMDb) Séries e Coleções
* **Contexto**: O TMDb expandiu significativamente sua base de séries televisivas e se consolidou como a principal alternativa aberta ao TheTVDB.
* **Necessidade**:
  * Habilitar suporte completo a séries (`TMDbTVClient`) como fonte primária de episódios, e não apenas filmes.
  * Suporte nativo ao binding `{collection}` / `{belongs_to_collection}` para organizar sagas e franquias cinematográficas em pastas conjuntas automaticamente.

### 1.4 AniDB e MyAnimeList (Anime Data)
* **Necessidade**:
  * Atualização dos esquemas de rate-limit da AniDB UDP API (evitar ban temporário por flood).
  * Suporte a nomes alternativos de episódios (romaji, kanji e títulos oficiais em inglês/português).

---

## 2. Novos Bindings de Formatação e Padrões de Naming

As versões recentes do FileBot adicionaram padrões integrados (*naming presets*) e variáveis avançadas de metadados:

### 2.1 Padrões Pré-definidos Prontos para Uso
* **`{plex}`**: Formato padrão do Plex Media Server:
  * Filmes: `Movies/Avatar (2009)/Avatar (2009) [1080p].mp4`
  * Séries: `TV Shows/Breaking Bad/Season 01/Breaking Bad - S01E01 - Pilot.mp4`
* **`{kodi}`**: Formato padrão para o Kodi/XBMC.
* **`{emby}`** e **`{jellyfin}`**: Compatibilidade total com os servidores de mídia open-source Emby e Jellyfin.

### 2.2 Metadados Avançados de Vídeo e Áudio
* **High Dynamic Range (HDR)**:
  * Identificação de perfis HDR: `{hdr}` (HDR10, HDR10+, Dolby Vision, HLG).
* **Codecs e Áudio Detalhado**:
  * `{videoCodec}`: Reconhecimento de AV1, HEVC/H.265, AVC/H.264, VP9.
  * `{audioLanguages}`: Lista formatada dos idiomas de faixas de áudio embutidas (ex: `[por, eng]`).
  * `{channels}`: Identificação correta de canais de áudio (ex: `7.1`, `5.1`, `2.0`).
  * `{audio.commercial}`: Identificação de codecs comerciais (Dolby Atmos, DTS-HD MA, TrueHD, EAC3).
* **Edições e Cortes Especiais**:
  * `{edition}`: Reconhecimento automático de edições de filmes (Extended Cut, Director's Cut, Remastered, IMAX, Unrated).

---

## 3. Melhorias na Linha de Comando (CLI) e Automação

### 3.1 Novas Ações do Sistema de Arquivos (`--action`)
* **`--action clone`**: Utilização de Copy-on-Write (reflink) instantâneo sem consumo extra de espaço em disco, suportado em sistemas de arquivos modernos:
  * Linux: Btrfs e XFS.
  * macOS: APFS.
  * Windows: ReFS.
* **`--action duplicate`**: Detecção e ação inteligente para criar links rígidos (*hardlinks*) ou cópias conforme o suporte do volume de destino.
* **`--action test`**: Modo *dry-run* aprimorado que simula todas as operações sem alterar arquivos.

### 3.2 Novas Opções de Pós-Processamento e Filtro
* **`--apply`**: Execução de ações auxiliares após a renomeação:
  * `--apply prune`: Limpeza de pastas vazias e sobras de arquivos inúteis (`.nfo`, `.url`, arquivos de amostra).
  * `--apply date`: Ajuste da data de modificação do arquivo para corresponder à data original de lançamento do filme ou episódio.
  * `--apply artwork`: Download automático de pôsteres, fanarts e logos.
* **`-exec`**: Execução de comandos arbitrários ou scripts externos recebendo o arquivo recém-processado como argumento.

### 3.3 Aprimoramentos no Script AMC (`amc.groovy`)
* Script central de automação para downloaders (qBittorrent, Transmission, Deluge):
  * Notificações automáticas via webhook (Discord, Telegram, Pushover).
  * Notificação direta a servidores Plex, Emby e Jellyfin para atualização instantânea da biblioteca.

---

## 4. Interface Gráfica do Usuário (UI/UX)

### 4.1 Modo Escuro (Dark Mode)
* Integração de tema escuro moderno via biblioteca [FlatLaf](https://www.formdev.com/flatlaf/).
* Detecção automática da preferência do sistema operacional (GTK no Linux, Dark Mode no Windows 10/11 e macOS) com opção manual de alternância nas configurações.

### 4.2 Suporte a Telas de Alta Resolução (HiDPI / 4K)
* Escalonamento automático de fontes e dimensões de componentes de interface.
* Migração de ícones bitmap (PNG estático) para gráficos vetoriais escaláveis (SVG) via FlatLaf SVG Icon.

### 4.3 Gerenciador de Presets do Usuário
* Interface gráfica dedicada para salvar, carregar, exportar e importar perfis customizados de renomeação (ex: "Filmes 4K HDR", "Anime Japonês com Legendas", "Séries em Português").

### 4.4 Ferramentas de Manutenção no Painel Filter
* "Mover para Lixeira" (*Move to Trash*) seguro integrado a ferramentas de filtragem.
* Localizador de duplicatas e identificador de episódios faltantes em temporadas incompletas.

---

## 5. Arquitetura e Remoção de Restrições Comerciais

* **Totalmente Open-Source e Livre de Nagware**:
  * As versões comerciais proprietárias adicionaram validação de chaves de licença pagas (`.psm`, `.p7b`), telas de aviso e popups coercitivos.
  * Esta implementação mantém o software 100% livre, transparente e aberto para a comunidade, sem limitações artificiais de recursos.
