# CLAUDE.md

Guia rápido para Claude Code (e outros agentes) trabalharem neste repositório de forma consistente. O detalhamento do produto vive em [`README.md`](README.md); este arquivo concentra **regras de execução**.

## O que é

App Android nativo (Kotlin + Jetpack Compose) que detecta padrões de candlestick em ativos da B3. Iniciativa originada como TCC do IFSP; atualmente em **transição para produto comercial** (lançamento Beta na Google Play planejado).

## Stack rápida

- Kotlin 2.2 / JDK 17 / Compose BOM 2025.06 / Material 3
- **Koin 4.1** (DI) — `di/AppModule.kt`
- **Room 2.7** (cache local) + **Firestore** + **Cloud Functions** + **Brapi API**
- Firebase Auth / App Check / Storage / Analytics
- Coil 2.7 para imagens

## Onde está o quê

- Detectores: `app/src/main/java/br/com/rodorush/chartpatterntracker/model/pattern/*Detector.kt` + `PatternDetectorRegistry.kt`
- Repositório principal: `model/CandlestickRepository.kt`
- Serviço Firestore: `model/FirestoreService.kt`
- ViewModels: `viewmodel/{Authentication,Chart,Screening}ViewModel.kt`
- Telas: `ui/screen/`
- DI: `di/AppModule.kt` + `model/DatabaseModule.kt`
- Providers (CompositionLocal — em rota para Koin via REFACT-05): `util/provider/`
- Strings i18n: `app/src/main/res/values{,-pt-rBR,-en-rUS,-es-rES}/strings.xml`
- Cloud Function: `functions/updateCandlesticks.js`

## Convenções deste repositório

- **Commits em PT-BR** seguindo Conventional Commits: `feat(pattern): ...`, `fix(chart): ...`, `refactor(app): ...`.
- **Strings de UI** sempre via `stringResource(R.string.*)` — três locales obrigatórios (pt-BR, en-US, es-ES).
- **Detectores**: 1 classe por padrão, sem herança; constantes de limite como `companion object` interno; comentário no topo da classe citando a fonte da regra (Nison, Bulkowski, etc.).
- **Test runner**: JUnit 4. Não introduzir Mockito, Turbine ou MockK sem decisão prévia.
- **Logs**: usar `br.com.rodorush.customlogs.Log` (`Log.d/w/e/i/v`) com `TAG` em `companion object` (em classes) ou `private const val TAG` no topo do arquivo (Composables/funções top-level). Nunca usar `android.util.Log` direto nem `kotlin.io.println` direto. Para debug rápido sem TAG, importar `br.com.rodorush.customlogs.println` (drop-in com TAG inferido do caller). Em release, `DEBUG`/`VERBOSE` são silenciados (incluindo o `println` da CustomLogs); `WARN`/`ERROR` com `Throwable` viram `recordException` no Crashlytics. Pares chave/valor passados ao final viram `setCustomKey` no Crashlytics — usar pra enriquecer contexto de erro (ticker, timeframe, etc.).

## Como rodar

- `./gradlew test` — testes unitários
- `./gradlew installDebug` — instalar no device
- Requer `local.properties` com `BRAPI_TOKEN`, `WEB_CLIENT_ID`, `FIREBASE_APP_CHECK_DEBUG_TOKEN` (e `sdk.dir`). Sem essas chaves o build falha em `app/build.gradle.kts`.
- Requer `app/google-services.json` (não versionado).

## Faça

- **Adicionar novo detector**:
  1. Criar classe em `model/pattern/`, implementando `PatternDetector`, com regra documentada em comentário (fonte: Nison/Bulkowski/etc.).
  2. Registrar em `PatternDetectorRegistry.init` com o ID numérico.
  3. Adicionar testes em `PatternDetectionTest.kt` (positivo, negativo limítrofe, boundary).
  4. Criar entrada do padrão no Firestore com nome/descrição/indicação/confiabilidade nos três locales.
  5. Adicionar drawable do ícone do padrão.
- **Antes de logar dados**: filtrar tokens, payloads completos e PII.
- **Antes de marcar uma task como concluída**: rodar `./gradlew test`.

## Não faça (regras endurecidas após auditoria de qualidade v1.0 → v1.1)

- **Nunca logar `BuildConfig.BRAPI_TOKEN`** ou maps/objetos que contenham o token. Já houve vazamento via `Log.d` em `CandlestickRepository`; a regra existe para não acontecer de novo. A CustomLogs em release silencia `DEBUG`/`VERBOSE` no logcat, mas isso é defesa em profundidade — `WARN`/`ERROR` continuam indo pro Crashlytics, então o token nunca pode aparecer em log de qualquer nível.
- **Nunca instanciar `Retrofit.Builder()` ou `FirebaseFirestore.getInstance()` dentro de Composables.** I/O fica no ViewModel/Repository. `REFACT-02` e `REFACT-04` ainda em aberto, mas a regra **já vale para código novo**.
- **Nunca usar `as` sem `?`** em payload de rede ou Firestore. Use `as?` + `filterIsInstance` ou desserialize com cuidado.
- **Nunca usar `!!`** quando `?.let { }`, `as?` ou `requireNotNull(...)` resolvem. `!!` em UI Compose é especialmente arriscado por causa de recomposição.
- **Não chamar `chartViewModel.fetchData` em loop** (race condition com `_patternsData` global). Para screening em lote use `chartViewModel.runScreening(...)` que é `suspend` e retorna direto.
- **Não engolir `CancellationException`** com `try { ... } catch (_: Exception) {}` em coroutines — quebra cancelamento estrutural. Se realmente precisa engolir, capture `CancellationException` separadamente e re-lance.
- **Não criar arquivo de documentação** (READMEs paralelos, NOTES.md etc.) sem o usuário pedir explicitamente.

## Roadmap vivo

Status, prioridades e ordem em [`ROADMAP.md`](ROADMAP.md). Antes de começar trabalho de fôlego: ler **Seção 1 (bugs conhecidos)** e **Seção 11 (refatoração arquitetural)** — a maior parte das próximas mudanças se encaixa lá. Atualize `[ ]` → `[~]` → `[x]` no mesmo PR que fecha o item.

## Memory

Memória pessoal do usuário em `~/.claude/projects/-mnt-c-Users-rodol-StudioProjects-ChartPatternTracker/memory/MEMORY.md`. Materiais físicos do TCC ficam em `G:\Meu Drive\Pós Graduação\TCC` (não acessível pelo WSL — pedir cópia para `C:\Temp\TCC` quando precisar; não ler `.json`).
