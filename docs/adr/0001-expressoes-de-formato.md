# ADR 0001 — Segurança das expressões de formato e filtros

**Status:** aceito · **Data:** 2026-10-03 · **Relaciona:** BUG-08, FIX-06

## Contexto

Expressões de formato (`{...}`) e filtros (`--filter`) são código Groovy vindo de
presets, linha de comando e formatos copiados de terceiros. Até o Java 23 elas
rodavam num sandbox do `SecurityManager` (`SecureCompiledScript`,
`PrivilegedInvocation`, `Policy` em `Main`): leitura de arquivos permitida;
escrita só nas pastas do app; sem rede, processos ou reflexão.

O Java 24+ removeu o `SecurityManager` (JEP 486). Hoje as expressões rodam com
privilégio total. Scripts completos (`-script`, AMC) são código confiável por
definição e ficam fora deste ADR.

## Opções

| Opção | Como | Prós | Contras |
|---|---|---|---|
| A. Aceitar e documentar | Remover o código morto e avisar sobre formatos de terceiros | Zero risco de quebrar formatos | Nenhuma proteção |
| B. `SecureASTCustomizer` | Verificação só na compilação | Sem dependência nova | Contornável por chamadas dinâmicas do Groovy |
| C. groovy-sandbox (Jenkins) | Reescreve toda chamada, propriedade e construtor para passar por um interceptador em tempo de execução, com política equivalente à antiga | Padrão maduro e robusto, alinhado à recomendação da OpenJDK (restringir no runtime da linguagem) | Dependência nova: `org.kohsuke:groovy-sandbox` 1.19 no Maven Central (Groovy 2.4.7) ou 1.34.x no repositório Jenkins; precisa de testes de compatibilidade com os formatos existentes |

Complementos independentes da opção: limite de tempo por expressão (`TimedInterrupt`
do Groovy), contra `while(true)`, e remoção de `Policy`/`SecurityManager`/`AccessController`.

## Recomendação

C + limite de tempo, com a política antiga: ler arquivos e metadados
permitido; bloquear processos (`execute`, `ProcessBuilder`, `Runtime`), rede,
escrita/exclusão fora das pastas do app, reflexão, `ClassLoader`, `System.exit`,
`GroovyShell`/`evaluate` e alteração de `metaClass`.

## Decisão

**Aceita a recomendação: opção C + limite de tempo.**

Implementar `groovy-sandbox` (`org.kohsuke:groovy-sandbox`) como interceptador
em tempo de execução, com a política antiga: ler arquivos e metadados permitido;
bloquear processos (`execute`, `ProcessBuilder`, `Runtime`), rede,
escrita/exclusão fora das pastas do app, reflexão, `ClassLoader`, `System.exit`,
`GroovyShell`/`evaluate` e alteração de `metaClass`.

Complementos:
- `TimedInterrupt` do Groovy por expressão (contra `while(true)`).
- Remover `Policy`/`SecurityManager`/`AccessController` de `Main`,
  `SecureCompiledScript` e `PrivilegedInvocation`, e renomear as classes para
  não sugerir um sandbox que não existe.
