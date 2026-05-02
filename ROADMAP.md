# Roadmap — ChartPatternTracker

Este documento consolida **bugs conhecidos**, **pendências técnicas**, **melhorias de UX/UI** e **novas funcionalidades** planejadas para o ChartPatternTracker. Os itens vêm das anotações de desenvolvimento (`Artefatos/Bugs e roteiro.txt` do TCC), de `TODO` no código-fonte e do trabalho futuro descrito na monografia.

**Status atual:** v1.0 — saída do TCC; em transição para v1.1 (Beta comercial), lançamento na Google Play Beta planejado.

**Como usar:**

- `[ ]` item pendente
- `[~]` item em andamento
- `[x]` item concluído

Para contribuir, abra uma **issue** referenciando o ID do item (ex.: `BUG-03`) ou um **PR** vinculado.

---

## Sumário

1. [Bugs conhecidos](#1-bugs-conhecidos)
2. [Pendências de detecção de padrões](#2-pendencias-de-deteccao-de-padroes)
3. [Melhorias de UX/UI](#3-melhorias-de-uxui)
4. [Novas funcionalidades — curto prazo](#4-novas-funcionalidades--curto-prazo)
5. [Novas funcionalidades — médio prazo](#5-novas-funcionalidades--medio-prazo)
6. [Novas funcionalidades — longo prazo](#6-novas-funcionalidades--longo-prazo)
7. [Publicação e produção](#7-publicacao-e-producao)
8. [Qualidade e testes](#8-qualidade-e-testes)
9. [Análises pendentes](#9-analises-pendentes)
10. [Como contribuir com o roadmap](#10-como-contribuir-com-o-roadmap)
11. [Refatoração arquitetural — próximos ciclos](#11-refatoracao-arquitetural--proximos-ciclos)

---

## 1. Bugs conhecidos

| ID | Item | Origem | Prioridade |
|----|------|--------|------------|
| BUG-01 | `[ ]` **Velas com valor 0 em alguns ativos.** Investigar se já vêm zeradas no JSON da Brapi ou se é erro de processamento. Definir estratégia de recuperação (re-fetch, descartar, marcar como inválido). | `Bugs e roteiro.txt` | Alta |
| BUG-02 | `[ ]` **Multiplicação por milissegundos nos timestamps.** Verificar a real necessidade dessa transformação no pipeline atual. | `Bugs e roteiro.txt` | Média |
| BUG-03 | `[ ]` **Regra do `lastUpdate` em `CandlestickEntity`.** Atualizar o registro **somente se** `lastUpdate == time`; se for maior, manter. Hoje a regra não está formalizada. | `Bugs e roteiro.txt` | Média |
| BUG-04 | `[ ]` **Previews do Compose quebram na IDE** quando há fontes de dados externas envolvidas. Refatorar para usar implementações mockadas via `LocalAuthProvider` / `LocalPatternProvider` em previews. | `Bugs e roteiro.txt` | Média |
| BUG-05 | `[ ]` **Log ruidoso `Unknown calling package name 'com.google.android.gms'`.** Issue conhecida do Google: <https://issuetracker.google.com/issues/381335071>. Documentar como ruído conhecido e filtrar nos logs. | `Bugs e roteiro.txt` | Baixa |

---

## 2. Pendências de detecção de padrões

| ID | Detector | Arquivo |
|----|----------|---------|
| PAT-01 | `[x]` Abandoned Baby Bearish | `model/pattern/AbandonedBabyBearishDetector.kt` |
| PAT-02 | `[x]` Bearish Engulfing | `model/pattern/BearishEngulfingDetector.kt` |
| PAT-03 | `[x]` Bullish Doji Star | `model/pattern/BullishDojiStarDetector.kt` |
| PAT-04 | `[x]` Bullish Hammer | `model/pattern/BullishHammerDetector.kt` |
| PAT-05 | `[x]` Evening Doji Star | `model/pattern/EveningDojiStarDetector.kt` |
| PAT-06 | `[x]` Evening Star | `model/pattern/EveningStarDetector.kt` |
| PAT-07 | `[x]` Harami Bullish | `model/pattern/HaramiBullishDetector.kt` |
| PAT-08 | `[x]` Three Outside Down | `model/pattern/ThreeOutsideDownDetector.kt` |
| PAT-09 | `[x]` Three Outside Up | `model/pattern/ThreeOutsideUpDetector.kt` |
| PAT-10 | `[x]` Harami Cross | `model/pattern/HaramiCrossDetector.kt` |

> Concluído no ciclo de qualidade de v1.1: regras validadas contra Nison/Bulkowski (com referência citada em comentário no topo de cada detector), constantes nomeadas em `companion object` e testes positivo/negativo/boundary para todos. Atualizações nas descrições dos padrões no Firestore continuam pendentes — serão tratadas em conjunto com `NF-02`.

---

## 3. Melhorias de UX/UI

| ID | Item | Origem |
|----|------|--------|
| UX-01 | `[ ]` **Chave para mostrar apenas padrões selecionados** na tela de resultados (filtro). | `Bugs e roteiro.txt` |
| UX-02 | `[ ]` **Toggle para mostrar/ocultar dados de volume** no gráfico. | `Bugs e roteiro.txt` |
| UX-03 | `[ ]` **Estrelas de confiabilidade sempre com 3 estrelas** (preenchidas + claras), seguindo o padrão Investing.com. | `Bugs e roteiro.txt` |
| UX-04 | `[ ]` **Melhorar visual do app** — paleta, espaçamento, animações, microinterações. | `Bugs e roteiro.txt` |
| UX-05 | `[ ]` **Skeletons de loading** em vez de spinners genéricos durante a varredura. | Sugestão TCC |
| UX-06 | `[ ]` **Mensagens de erro com ações de recuperação** (retry, sugestões). | Sugestão TCC |
| UX-07 | `[ ]` **Tooltips/onboarding** para novos usuários (introdução guiada). | Sugestão TCC |
| UX-08 | `[ ]` **Auditoria de acessibilidade** (alvos de toque, leitores de tela, contraste WCAG AA). | Sugestão TCC |

---

## 4. Novas funcionalidades — curto prazo

| ID | Item | Origem |
|----|------|--------|
| NF-01 | `[ ]` **Acrescentar mais padrões de candlestick.** Catálogo conceitual prevê 70+; hoje há 10 implementados. | `Bugs e roteiro.txt` |
| NF-02 | `[ ]` **Validar nomes dos padrões em inglês e espanhol.** A localização atual prioriza pt-BR. | `Bugs e roteiro.txt` |
| NF-03 | `[ ]` **Lembrar senha / alterar senha / alterar dados / link de confirmação de e-mail.** Cloud Function preparada, falta integração no front. | `Bugs e roteiro.txt` |
| NF-04 | `[ ]` **Token da Brapi via variável de ambiente** com `lateinit`, em vez de `BuildConfig`. Reduz risco de vazamento via APK. | `Bugs e roteiro.txt` |
| NF-05 | `[ ]` **Tela de Disclaimer** sobre risco de operações no mercado financeiro. | `Bugs e roteiro.txt` |
| NF-06 | `[ ]` **Fonte de dados internacional** ativando o `AlphaVantageApi` (já parcialmente implementado em `data/AlphaVantageApi.kt`). | Código |

---

## 5. Novas funcionalidades — médio prazo

| ID | Item |
|----|------|
| NF-07 | `[ ]` **Análise multi-timeframe** (alinhamento de sinais entre diário e semanal, p. ex.). |
| NF-08 | `[ ]` **Estatísticas históricas de taxa de sucesso** por padrão e por ativo. |
| NF-09 | `[ ]` **Watchlists** e screenings salvos por usuário (Firestore `users/{uid}/watchlists`). |
| NF-10 | `[ ]` **Notificações push** de alertas de padrões via FCM + Cloud Functions. |
| NF-11 | `[ ]` **Indicadores técnicos clássicos** (médias móveis, RSI, MACD) sobrepostos ao gráfico. |
| NF-12 | `[ ]` **Tema customizável** (cores de candle, paleta personalizada). |

---

## 6. Novas funcionalidades — longo prazo

| ID | Item |
|----|------|
| NF-13 | `[ ]` **Backtesting** de padrões em dados históricos. |
| NF-14 | `[ ]` **Versão iOS** (SwiftUI) com mesmo backend Firebase. |
| NF-15 | `[ ]` **Dashboard web** (Next.js) para análise em tela maior. |
| NF-16 | `[ ]` **Integração com corretoras** (paper trading inicialmente, real depois). |
| NF-17 | `[ ]` **Marketplace de conteúdo educacional** (cursos, tutoriais, padrões premium). |
| NF-18 | `[ ]` **Modelo freemium / assinatura premium** (alertas em tempo real, padrões avançados). |
| NF-19 | `[ ]` **API pública** para desenvolvedores (REST autenticada). |
| NF-20 | `[ ]` **Recursos sociais** — compartilhar resultados, seguir traders. |

---

## 7. Publicação e produção

| ID | Item | Origem |
|----|------|--------|
| PROD-01 | `[ ]` **Integrar Firebase Crashlytics** + CustomLog para monitoramento de erros. | `Bugs e roteiro.txt` |
| PROD-02 | `[ ]` **Habilitar ofuscamento (ProGuard/R8).** Atualmente `isMinifyEnabled = false` em `app/build.gradle.kts:57`. | `Bugs e roteiro.txt` |
| PROD-03 | `[ ]` **Adicionar propagandas** (AdMob) na tier gratuita. | `Bugs e roteiro.txt` |
| PROD-04 | `[ ]` **Configurar signing keystore** para release na Play Store. | Padrão Android |
| PROD-05 | `[ ]` **Configurar Firebase App Check Play Integrity** corretamente em produção. | Código |
| PROD-06 | `[ ]` **Privacy Policy** e Termos de Uso publicados. | Compliance |
| PROD-07 | `[ ]` **CI/CD** com GitHub Actions ou Bitrise (build, test, lint, deploy). | DevOps |
| PROD-08 | `[ ]` **Cloud Scheduler** para pré-popular Firestore com ativos populares (reduz latência inicial). | Sugestão TCC |

---

## 8. Qualidade e testes

| ID | Item |
|----|------|
| QA-01 | `[x]` **Testes unitários para os 10 detectores** (positivo, negativo limítrofe e boundary; 10/10 cobertos). |
| QA-02 | `[ ]` **Testes de UI com `ComposeTestRule`** para fluxos críticos (login, screening, gráfico). |
| QA-03 | `[ ]` **Testes de integração** Firestore + Room. |
| QA-04 | `[ ]` **Resiliência de API** — retry com backoff exponencial em `BrapiDataSource`. |
| QA-05 | `[ ]` **Validação de payload** mais robusta após mudanças quebradoras da Brapi. |
| QA-06 | `[ ]` **Métricas de performance** das funções (tempo de execução, memória). |
| QA-07 | `[ ]` **Lint configurado** com regras consistentes (Detekt, ktlint). |

---

## 9. Análises pendentes

Itens de investigação levantados em `Bugs e roteiro.txt` que não são bugs claros, mas exigem decisão técnica:

- `[ ]` **Origem das velas zeradas:** Brapi devolve `0` legitimamente? Em quais ativos? Em quais timeframes? Contatar o suporte da Brapi.
- `[ ]` **Estratégia de atualização incremental:** quando uma vela existente diverge da remota, atualizar ou manter? Definir política.
- `[ ]` **Limpeza periódica do banco local:** hoje não há job que descarte velas antigas. Definir TTL (ex.: 30 dias) e implementar `clearOldData()`.
- `[ ]` **Cache de logos da Brapi:** revisar política do Coil para reduzir tráfego.
- `[ ]` **Migração para Hilt?** Avaliar custo/benefício de trocar Koin por Hilt (atualmente Koin atende bem).

---

## 10. Como contribuir com o roadmap

1. Abra uma issue no repositório com o título contendo o ID do item (ex.: `[BUG-01] Velas com valor 0`).
2. Para sugerir um item novo, use o template `feature_request` ou `bug_report` (a criar).
3. Vincule o PR ao item do roadmap na descrição.
4. Atualize o estado neste arquivo (`[ ]` → `[~]` → `[x]`) no mesmo PR.
5. Itens concluídos podem ser movidos para uma seção `## Histórico` com a data.

> A ordenação dentro de cada seção reflete uma sugestão de prioridade, mas é flexível — discuta no canal apropriado antes de pular itens críticos.

---

## 11. Refatoração arquitetural — próximos ciclos

Itens documentados durante a auditoria de qualidade de v1.0 → v1.1. **Não foram executados naquele ciclo** (escopo era saneamento mínimo); ficam aqui para os próximos sprints, com prioridade para os que destravam testabilidade e isolam o backend.

| ID | Item | Motivação |
|----|------|-----------|
| REFACT-01 | `[ ]` **Camada de domínio com Use Cases.** Extrair `StartScreeningUseCase`, `EnrichAssetUseCase`, `DetectPatternsUseCase` a partir do que hoje está em `ScreeningViewModel.startScreening` e em `ChartViewModel.fetchData`/`runScreening`. ViewModels passam a coordenar e expor estado. | Hoje os ViewModels concentram orquestração + I/O + formatação, dificultando testes e violando SRP. |
| REFACT-02 | `[ ]` **Singleton `Retrofit` via Koin.** Hoje `Retrofit.Builder()` aparece em `ScreeningViewModel`, `BrapiDataSource` e `BrapiAssetsProvider`. Criar `single { retrofitBrapi() }` em `AppModule` e injetar. | Reduz alocações duplicadas, alinha com o restante da DI e prepara o terreno para interceptors (logging, retry, App Check). |
| REFACT-03 | `[ ]` **`sealed class` para UiState.** Substituir os múltiplos `StateFlow` paralelos (`isLoading`, `error`, `data`) em `ScreeningViewModel` e `ChartViewModel` por estados consolidados (Loading/Success/Error). | Remove combinações inválidas (loading=true com error≠null) e simplifica os Composables. |
| REFACT-04 | `[ ]` **Abstrair Firebase atrás de interfaces de repositório.** Hoje `FirebaseFirestore.getInstance()` aparece direto em `ScreeningViewModel`, `SelectAssetScreen`, `FirestoreService`, `FirebasePatternProvider`. Introduzir `AssetRepository`, `PatternRepository` etc. e fazer Firebase virar detalhe de implementação. | Pré-requisito para testar ViewModels sem rede e para trocar de backend no futuro. |
| REFACT-05 | `[ ]` **Unificar Providers em Koin.** `AuthProvider`, `PatternProvider`, `AssetsProvider` hoje vivem como `CompositionLocal` paralelo ao Koin. Migrar para Koin com qualifier `mock` vs `real`. | Elimina dois sistemas de DI concorrentes; troca de implementação em testes/dev fica padronizada. |
| REFACT-06 | `[ ]` **Mappers explícitos entre camadas (Entity → Domain → UI).** Hoje `Candlestick`, `PatternItem`, `AssetItem` são reusados em data/domain/UI. | Evita vazamento de modelos e permite evoluir camadas independentemente (renomear, anotar Room sem afetar UI etc.). |
| REFACT-07 | `[ ]` **`PatternDetector.status` (enum `VALIDATED/EXPERIMENTAL`).** Substituir o antigo `//TODO Confirmar regras` (já removido) por um marcador formal por detector, permitindo filtrar experimentais em produção. | Quando o catálogo crescer (alvo: 70+ padrões), nem todos terão sido validados ao mesmo tempo — precisamos de um gate sem comentar código. |
| REFACT-08 | `[ ]` **Logging utilitário com nível por build.** Hoje `Log.d` é compilado em release. Adicionar wrapper que respeite `BuildConfig.DEBUG` e/ou nível configurável, evitando logs verbosos em produção. | Preparação para release comercial; também complementa a regra "nunca logar tokens" do `CLAUDE.md`. |
