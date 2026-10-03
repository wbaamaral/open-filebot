# Design tokens e ícones

A aparência do Open FileBot é definida por **tokens de design**: valores nomeados
para espaçamento, raio, tamanhos de ícone, cores e tipografia. O código usa
apenas os nomes; os valores ficam em folhas de estilo, uma base e uma por tema.
Assim, uma mudança visual é feita em um único lugar e vale para toda a interface,
nos temas claro, escuro e clássico.

## Onde ficam

| Arquivo | Conteúdo |
|---|---|
| [`source/net/filebot/theme/FlatLaf.properties`](../source/net/filebot/theme/FlatLaf.properties) | Tokens comuns a todos os temas e classes de estilo `fb-*` |
| [`source/net/filebot/theme/FlatLightLaf.properties`](../source/net/filebot/theme/FlatLightLaf.properties) | Valores do tema claro |
| [`source/net/filebot/theme/FlatDarkLaf.properties`](../source/net/filebot/theme/FlatDarkLaf.properties) | Valores do tema escuro |
| [`source/net/filebot/util/ui/Tokens.java`](../source/net/filebot/util/ui/Tokens.java) | Nomes dos tokens e acesso tipado no código |
| [`source/net/filebot/resources/svg/colors.properties`](../source/net/filebot/resources/svg/colors.properties) | Papéis de cor dos ícones → tokens |
| [`tools/icons/icons.tsv`](../tools/icons/icons.tsv) | Mapeamento dos ícones (nome → ícone Tabler → papel de cor → tamanho) |

As folhas são registradas no FlatLaf ao aplicar a aparência. No tema Clássico
(Nimbus), que não lê folhas do FlatLaf, `Tokens.installFallback` carrega os
valores literais das mesmas folhas.

## Tokens

### Espaçamento, raio e ícones (px)

| Token | Valor | Uso |
|---|---|---|
| `FileBot.space.xs` / `sm` / `md` / `lg` / `xl` | 4 / 8 / 12 / 16 / 24 | Margens e espaços entre componentes |
| `FileBot.radius.sm` / `md` / `lg` | 6 / 8 / 12 | Caixas, cartões e blocos de ícone |
| `FileBot.icon.small` / `medium` / `large` | 16 / 24 / 48 | Tamanhos de ícone |

### Cores

| Token | Significado |
|---|---|
| `FileBot.textColor`, `FileBot.mutedColor` | Texto principal e texto secundário |
| `FileBot.accentColor`, `FileBot.linkColor` | Destaque e links |
| `FileBot.borderColor`, `FileBot.separatorColor` | Bordas e separadores |
| `FileBot.surfaceColor`, `FileBot.surfaceAltColor`, `FileBot.surfaceRaisedColor` | Fundo da janela, caixas de ajuda e cartões/campos |
| `FileBot.successColor`, `FileBot.warningColor`, `FileBot.dangerColor`, `FileBot.infoColor` | Estados |
| `FileBot.header.edgeColor`, `FileBot.header.centerColor` | Gradiente do cabeçalho dos painéis |
| `FileBot.panel.<painel>Color` | Cor de identidade de cada painel (barra lateral) |
| `FileBot.icon.glyphColor`, `FileBot.icon.onTileColor` | Cor padrão dos ícones e do símbolo sobre os blocos |

### Classes de estilo

| Classe | Uso |
|---|---|
| `fb-title`, `fb-heading`, `fb-caption`, `fb-badge` | Título, seção, legenda e selos |
| `fb-code`, `fb-code-small` | Expressões de formato (JetBrains Mono) |
| `fb-info` | Caixas de ajuda e informação |
| `fb-card` | Cartões elevados (ex.: pré-visualização) |
| `fb-header` | Cabeçalhos de diálogos |
| `fb-list-surface` | Painéis que continuam a superfície de uma tabela |

No código, os papéis tipográficos são aplicados com
`Appearance.Typography.<PAPEL>.apply(componente)` e as superfícies com
`Tokens.styleClass(componente, Tokens.STYLE_INFO)`.

## Regras

1. **Nunca use valores literais** de cor, tamanho de fonte, espaçamento ou raio
   no código de interface; use um token existente ou crie um novo.
2. **Nomes seguem o FlatLaf**, que deduz o tipo pelo sufixo: cores terminam em
   `Color`; espaçamento, raio e tamanho são inteiros.
3. **Classes de estilo usam `$token`**: variáveis `@` e tokens dentro de listas
   numéricas de borda não são resolvidos. Bordas compostas são classes Java que
   leem os tokens (ex.: `TokenBorder`).
4. **Todo token novo precisa existir em todos os temas.** O teste
   `TokensTest` verifica isso para Claro, Escuro e Clássico.

## Ícones

Os ícones de interface são SVG gerados a partir do [Tabler Icons](https://tabler.io/icons)
(MIT, versão fixada no gerador). Cada SVG usa **cores-marcador** que o
`ResourceManager` substitui, na hora de desenhar, pelo token do papel
correspondente. Assim, os ícones acompanham o tema sem arquivos separados.

| Papel | Token |
|---|---|
| `glyph` | `FileBot.icon.glyphColor` |
| `accent`, `success`, `warning`, `danger`, `info`, `muted` | Cores semânticas correspondentes |
| `tile:<painel>` | Bloco colorido em `FileBot.panel.<painel>Color` com símbolo em `FileBot.icon.onTileColor` |

Para adicionar ou trocar um ícone:

1. Edite [`tools/icons/icons.tsv`](../tools/icons/icons.tsv) (nome, ícone Tabler, papel e tamanho).
2. Rode `python3 tools/icons/generate-icons.py`.
3. Rode `ant test`: o `IconsTest` verifica se todos os ícones carregam e usam apenas cores-marcador.

Logos de serviços (`search.*`), bandeiras e o ícone do aplicativo continuam em
PNG; quando não há SVG com o nome pedido, o `ResourceManager` usa o PNG.
