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
12. [NF-03 — esboço de implementação](#12-nf-03--esboco-de-implementacao)

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
| NF-03 | `[~]` **Lembrar senha / alterar senha / alterar e-mail / alterar dados de conta.** Plano detalhado em **§12 (NF-03 — esboço de implementação)**. **Sub-item NF-03a entregue (2026-05-05)**: `sendPasswordResetEmail` no `AuthProvider`/`FirebaseAuthProvider`/`MockAuthProvider`, `ResetPasswordStatus` sealed + `sendPasswordReset` no `AuthenticationViewModel` (cooldown 60s reaproveitando `armResendCooldown`), nova `ForgotPasswordScreen` acessível via link "Esqueci minha senha" no `LoginScreen` e rota `forgot_password` em `AuthenticationNavHost`, strings nos 3 locales, 3 testes novos no `AuthenticationViewModelTest` (sucesso/falha/cooldown). Mensagem pós-envio é genérica para casar com email enumeration protection. **Sub-item NF-03e entregue (2026-05-25)**: exclusão de conta in-app — como **não há dados por usuário no Firestore**, é só `FirebaseUser.delete()` + reauth (sem limpeza de coleções). Novos métodos `deleteCurrentUser`/`reauthenticateWithPassword`/`reauthenticateWithGoogle` no `AuthProvider`/`FirebaseAuthProvider`/`MockAuthProvider`, `DeleteAccountStatus` sealed + `deleteAccount`/`reauthenticateAndDelete*`/`resetDeleteAccountStatus` no `AuthenticationViewModel`, botão "Excluir conta" na `SettingsScreen` com diálogo de confirmação e diálogo de reauth (senha ou relançar Google Sign-In em `requires-recent-login`); pós-exclusão redireciona via `onLogout` (volta à `AuthenticationActivity`). Strings nos 3 locales, 5 testes novos no `AuthenticationViewModelTest`. **Pendente (manual)**: declarar exclusão de conta na **Play Console** (seção *Data deletion*) + publicar **URL web** de solicitação de exclusão — casa com **PROD-06** (Privacy Policy). **Sub-itens NF-03b/c/d entregues (2026-05-25)**: gestão de conta in-app numa nova `ProfileScreen` (rota `profile`) com e-mail/nome/provedor + botões que abrem diálogos para **alterar nome** (`updateProfile`, sem reauth), **alterar senha** (`updatePassword`) e **alterar e-mail** (`verifyBeforeUpdateEmail` — link vai ao novo endereço; troca só efetiva após o clique). Novos métodos `updatePassword`/`verifyBeforeUpdateEmail`/`updateDisplayName`/`currentUserDisplayName` no `AuthProvider`/`FirebaseAuthProvider`/`MockAuthProvider`; sealed `UpdateNameStatus`/`UpdatePasswordStatus`/`UpdateEmailStatus` + funções (e `reauthAndUpdate{Password,Email}With{Password,Google}`) no `AuthenticationViewModel`, reaproveitando os helpers de reauth do NF-03e. A **exclusão de conta migrou da `SettingsScreen` para a `ProfileScreen`** (a Settings agora só tem botão "Perfil"); o diálogo de reauth (senha ou relançar Google Sign-In) é compartilhado entre exclusão/senha/e-mail via um `ReauthOp` pendente. Para contas Google (`provider == "google.com"`), **alterar nome continua disponível** (`updateProfile` persiste mesmo em conta Google); senha e e-mail ficam ocultos (§12.7, geridos na conta Google, com nota apontando pro myaccount.google.com) e a exclusão segue exposta. Strings nos 3 locales (+ `confirm`/`cancel`/`save` genéricos), 10 testes novos no `AuthenticationViewModelTest`. **Pendente (manual)**: customizar no Firebase Console o template de **alteração de e-mail** (gerado por `verifyBeforeUpdateEmail`) nos 3 locales — alinhado a NF-02/NF-21. | `Bugs e roteiro.txt` |
| NF-21 | `[~]` **Verificação de email no cadastro — fase 1 (Firebase Auth nativo).** Código entregue: `FirebaseAuthProvider.sendEmailVerification` disparado após `createUserWithEmailAndPassword`, `VerifyEmailScreen` com botão **Reenviar** (cooldown local de 60 s) e botão "Já verifiquei", gate em `AuthenticationActivity` que redireciona usuários `password` não verificados para a tela de verificação, auto-recheck via `LifecycleEventObserver` no `ON_RESUME`, isenção do Google Sign-In (provider já garante o e-mail), strings i18n nos 3 locales, testes unitários do ViewModel (6/6). Action URL fica como **handler padrão do Firebase** (browser → "E-mail verificado" → usuário volta ao app); **Android App Link** com deep link adiado para fase 1.5. **Email enumeration protection habilitada e verificada (2026-06-11)** — `accounts:createAuthUri` não revela mais se o e-mail está registrado. **Pendente (passo manual no Firebase Console)**: customizar template de verificação nos três locales (pt-BR, en-US, es-ES) — alinhado com `NF-02`; atenção: customizar o corpo no Console desliga a localização automática do Firebase (avaliar manter template padrão até `NF-22`). | Beta comercial — pré-requisito de qualidade/segurança no onboarding. |
| NF-22 | `[ ]` **Verificação de email — fase 2 (Cloud Function + template próprio).** **Só executar se** branding total do email ou métricas de entrega/bounce virarem requisito. Migrar para `auth().generateEmailVerificationLink()` + envio via SendGrid/Resend; Firestore guardando `lastVerificationEmailSentAt` e contador de tentativas para rate limit server-side. | Diferimento explícito: enquanto o template do Console atender, evita custo de SendGrid e código a manter. |
| NF-04 | `[ ]` **Token da Brapi via variável de ambiente** com `lateinit`, em vez de `BuildConfig`. Reduz risco de vazamento via APK. | `Bugs e roteiro.txt` |
| NF-05 | `[x]` **Tela de Disclaimer** sobre risco de operações no mercado financeiro. Gate de "Li e concordo" no primeiro uso (aceite persistido em `AppPreferences`/`SharedPreferences`), ancorado no `AppNavHost` pós-login como `startDestination` quando `!disclaimerAccepted`; reacessível em modo leitura via card em `SettingsScreen` (rota `disclaimer?readOnly={readOnly}`). Nova `DisclaimerScreen`, `util/AppPreferences.kt` (single Koin reusando o store `app_prefs`), strings nos 3 locales + default. | `Bugs e roteiro.txt` |
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
| PROD-01 | `[x]` **Integrar Firebase Crashlytics** + CustomLog para monitoramento de erros. Entregue junto com `REFACT-08`: em release a CustomLogs planta `CrashlyticsTree` (WARN/ERROR + `Throwable` viram `recordException`; pares chave/valor viram `setCustomKey`). | `Bugs e roteiro.txt` |
| PROD-02 | `[x]` **Ofuscamento (ProGuard/R8) habilitado.** `isMinifyEnabled = true` + `isShrinkResources = true` no `buildTypes.release`; keep rules em `proguard-rules.pro` para DTOs Gson da Brapi (`api.**`), campos `@SerializedName` e `PatternItem` (Firestore `toObject`). `assembleRelease` valida o build (`minifyReleaseWithR8` + `lintVitalRelease` OK). | `Bugs e roteiro.txt` |
| PROD-03 | `[ ]` **Adicionar propagandas** (AdMob) na tier gratuita. | `Bugs e roteiro.txt` |
| PROD-04 | `[~]` **Signing config para release.** `build.gradle.kts` lê `keystore.properties` (não versionado) e aplica `signingConfigs.release` com fallback gracioso (sem o arquivo, release sai não assinado e o build não quebra). `.gitignore` cobre `*.jks`/`*.keystore`/`keystore.properties`. **Pendente (manual)**: gerar a upload key (`keytool`), criar `keystore.properties` e habilitar **Play App Signing** no Console. | Padrão Android |
| PROD-05 | `[x]` **App Check Play Integrity em produção.** `ChartPatternApplication` agora seleciona o provider por `BuildConfig.DEBUG`: Debug em debug, `PlayIntegrityAppCheckProviderFactory` em release (antes instalava o provider Debug incondicionalmente — backend sem proteção real em produção). **Pendente (manual)**: registrar o SHA-256 da release e habilitar/enforçar Play Integrity no App Check do Console. | Código |
| PROD-06 | `[~]` **Privacy Policy + Termos de Uso.** Documentos trilíngues (PT/EN/ES) em `hosting/{privacy,terms,index}.html` servidos por **Firebase Hosting** (`firebase.json` + `.firebaserc`, projeto `chartpatterntracker`); a Política tem seção `#data-deletion` que casa com a exclusão de conta in-app (NF-03e). No app: `util/LegalUrls.kt` (fonte única das URLs), checkbox do `RegisterScreen` com "Termos de Uso"/"Política de Privacidade" clicáveis (`LinkAnnotation.Url` + `withLink`), card "Documentos legais" na `SettingsScreen` (abre via `LocalUriHandler`), strings nos 3 locales + default (substituindo `i_understood_the_terms_policy`). **Consentimento unificado:** o aceite de Termos/Política foi embutido no gate universal de aviso de risco (`DisclaimerScreen` via `LegalAgreementText`), persistido em `disclaimerAccepted` — cobre também quem entra via Google (que não passa pelo checkbox do cadastro). O Google Sign-In virou funcional no `RegisterScreen` (`SocialSignInButton` + `rememberGoogleSignInLauncher`, helper extraído e reusado com o `LoginScreen`). **Deploy concluído e verificado (2026-06-11)**: `privacy`/`terms`/`index` no ar em `chartpatterntracker.web.app`, conteúdo idêntico ao repo, âncora `#data-deletion` presente. **Pendente (manual)**: revisão jurídica dos textos; colar a Privacy Policy URL (`https://chartpatterntracker.web.app/privacy`) na ficha da Play Store e a URL `https://chartpatterntracker.web.app/privacy#data-deletion` na Play Console (seção *Data deletion*). | Compliance |
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
| REFACT-03 | `[ ]` **`sealed class` para UiState.** Substituir os múltiplos `StateFlow` paralelos (`isLoading`, `error`, `data`) em `ScreeningViewModel` e `ChartViewModel` por estados consolidados (Loading/Success/Error). `AuthenticationViewModel` já tem `VerificationStatus` como sealed class após NF-21 — usar como modelo. | Remove combinações inválidas (loading=true com error≠null) e simplifica os Composables. |
| REFACT-04 | `[ ]` **Abstrair Firebase atrás de interfaces de repositório.** Hoje `FirebaseFirestore.getInstance()` aparece direto em `ScreeningViewModel`, `SelectAssetScreen`, `FirestoreService`, `FirebasePatternProvider`. Introduzir `AssetRepository`, `PatternRepository` etc. e fazer Firebase virar detalhe de implementação. | Pré-requisito para testar ViewModels sem rede e para trocar de backend no futuro. |
| REFACT-05 | `[ ]` **Unificar Providers em Koin.** `AuthProvider`, `PatternProvider`, `AssetsProvider` hoje vivem como `CompositionLocal` paralelo ao Koin. Migrar para Koin com qualifier `mock` vs `real`. | Elimina dois sistemas de DI concorrentes; troca de implementação em testes/dev fica padronizada. |
| REFACT-06 | `[ ]` **Mappers explícitos entre camadas (Entity → Domain → UI).** Hoje `Candlestick`, `PatternItem`, `AssetItem` são reusados em data/domain/UI. | Evita vazamento de modelos e permite evoluir camadas independentemente (renomear, anotar Room sem afetar UI etc.). |
| REFACT-07 | `[ ]` **`PatternDetector.status` (enum `VALIDATED/EXPERIMENTAL`).** Substituir o antigo `//TODO Confirmar regras` (já removido) por um marcador formal por detector, permitindo filtrar experimentais em produção. | Quando o catálogo crescer (alvo: 70+ padrões), nem todos terão sido validados ao mesmo tempo — precisamos de um gate sem comentar código. |
| REFACT-08 | `[x]` **Logging utilitário com nível por build.** Substituído `android.util.Log` por `br.com.rodorush.customlogs.Log` (library `io.github.rodorush:customlogs`, ver `REFACT-09`). Em debug planta `DebugTree` (logcat); em release planta `CrashlyticsTree` (silencia DEBUG/VERBOSE; WARN/ERROR + Throwable viram `recordException` e pares chave/valor viram `setCustomKey`). Crashlytics integrado ao app no mesmo PR. | Preparação para release comercial; também complementa a regra "nunca logar tokens" do `CLAUDE.md`. |
| REFACT-09 | `[x]` **CustomLogs publicada no Maven Central.** A library foi perdida com a máquina antiga e reconstruída a partir dos call sites deste repo; vive em `Rodorush/CustomLogs`, sob Apache-2.0, publicada como `io.github.rodorush:customlogs:0.1.0`. O `includeBuild("../CustomLogs")` saiu; o app consome `implementation(libs.customlogs)`. | Desacopla o app da pasta sibling; permite consumir a mesma library em outros projetos sem duplicar fonte. Pública e com licença livre por decisão do autor. |

---

## 12. NF-03 — esboço de implementação

Plano detalhado para quando NF-03 entrar em prioridade. Sub-itens são independentes — podem ir em PRs separados.

### 12.1 Sub-escopos

| Sub-ID | Fluxo | Endpoint Firebase | Onde mora |
|--------|-------|-------------------|-----------|
| NF-03a | **Esqueci minha senha** (não autenticado) | `auth.sendPasswordResetEmail(email)` | Link em `LoginScreen` → nova `ForgotPasswordScreen` |
| NF-03b | **Alterar senha** (autenticado, e-mail/senha) | `user.updatePassword(newPassword)` (com reauth) | Nova `ChangePasswordScreen` em Settings/Profile |
| NF-03c | **Alterar e-mail** (autenticado) | `user.verifyBeforeUpdateEmail(newEmail)` (com reauth) | Nova `ChangeEmailScreen` em Settings/Profile |
| NF-03d | **Alterar `displayName`** (autenticado) | `user.updateProfile(UserProfileChangeRequest)` | Mesma tela de edição de perfil |
| NF-03e | **Excluir conta** (LGPD) | `user.delete()` (com reauth) | Settings/Profile, escondido atrás de confirmação |

`updateEmail` foi **descontinuado** em 2024 — sempre usar `verifyBeforeUpdateEmail`, que envia link para o novo endereço e só efetiva a troca após clique. Mesmo modelo do NF-21.

### 12.2 Reautenticação recente

`updatePassword`, `verifyBeforeUpdateEmail` e `delete` exigem **sessão recente** (`requires-recent-login`). Se a sessão expirou, é preciso reautenticar antes:

- **E-mail/senha**: `EmailAuthProvider.getCredential(email, password)` → `user.reauthenticate(credential)`.
- **Google**: relançar `GoogleSignInClient` para obter `idToken` novo → `GoogleAuthProvider.getCredential(idToken, null)` → `user.reauthenticate(credential)`.

Centralizar como helpers no `AuthProvider`:

```kotlin
fun reauthenticateWithPassword(password: String, onResult: (Boolean, String?) -> Unit)
fun reauthenticateWithGoogle(idToken: String?, onResult: (Boolean, String?) -> Unit)
```

Cada tela "destrutiva" tenta a operação direto e, ao receber `FirebaseAuthRecentLoginRequiredException`, abre um diálogo pedindo a senha (ou reabrindo o Google Sign-In) antes de retentar.

### 12.3 AuthProvider — métodos a adicionar

```kotlin
// Não autenticado
fun sendPasswordResetEmail(email: String, onResult: (Boolean, String?) -> Unit)

// Autenticado — exigem sessão recente
fun updatePassword(newPassword: String, onResult: (Boolean, String?) -> Unit)
fun verifyBeforeUpdateEmail(newEmail: String, onResult: (Boolean, String?) -> Unit)
fun updateDisplayName(displayName: String, onResult: (Boolean, String?) -> Unit)
fun deleteCurrentUser(onResult: (Boolean, String?) -> Unit)

// Helpers de reautenticação
fun reauthenticateWithPassword(password: String, onResult: (Boolean, String?) -> Unit)
fun reauthenticateWithGoogle(idToken: String?, onResult: (Boolean, String?) -> Unit)
```

`MockAuthProvider` ganha implementações controláveis (mesmo padrão do NF-21).

### 12.4 Telas e navegação

- **`LoginScreen`**: link "Esqueci minha senha" abaixo do campo Senha → navega para `ForgotPasswordScreen`.
- **`ForgotPasswordScreen`** (novo): input de e-mail + botão Enviar. Sempre mostra mensagem genérica de sucesso (a *email enumeration protection* já está habilitada — não vazar se a conta existe).
- **`ProfileScreen`** (novo, em Settings): exibe e-mail, displayName, provedor; botões para alterar senha (só se provedor = `password`), alterar e-mail, alterar nome, excluir conta.
- **`ChangePasswordScreen`**: senha atual + nova + confirmar. Tenta `updatePassword`; em `requires-recent-login`, dispara reauth.
- **`ChangeEmailScreen`**: novo e-mail. Após sucesso, mostra "Verifique sua caixa de entrada no novo endereço" (a troca só efetiva após o clique no link).
- **Rotas novas no `AppNavHost`**: `forgot_password`, `profile`, `change_password`, `change_email`. (`forgot_password` precisa estar acessível de dentro do `AuthenticationNavHost` também.)

### 12.5 Strings i18n (4 locales)

```
forgot_password, forgot_password_title, forgot_password_send,
forgot_password_sent_generic
change_password_title, current_password, new_password, password_updated
change_email_title, new_email, email_change_pending,
verify_new_email_check_inbox
change_display_name_title, display_name
delete_account_title, delete_account_warning, delete_account_confirm
reauth_required, reauth_password_prompt, reauth_with_google
```

Reaproveitar `verify_email_send_error`, `network_error`, `logout`.

### 12.6 Templates no Firebase Console

Action URL fica como **handler padrão do Firebase** (mesmo do NF-21). Templates a customizar nos 3 locales:

- **Redefinição de senha** (já existe no Console, padrão Firebase).
- **Alteração de endereço de e-mail** (gerado pelo `verifyBeforeUpdateEmail`).

### 12.7 Provedores Google: o que esconder

Usuários Google (provider `google.com`) **não têm senha local** — `updatePassword` e `sendPasswordResetEmail` falham silenciosamente para eles. Em `ProfileScreen`:

- Esconder "Alterar senha".
- Para alterar e-mail/nome, recomendar via conta Google (link externo). Tecnicamente é possível chamar `verifyBeforeUpdateEmail` mesmo em conta Google, mas vira um e-mail/senha manual — comportamento confuso. Adiar até decidirmos UX.
- "Excluir conta" continua disponível (com reauth via Google).

### 12.8 LGPD / "Excluir conta"

`PROD-06` (Privacy Policy) lista que o usuário pode pedir exclusão via console. Implementar `deleteCurrentUser` formaliza isso no app — exigência crescente para Play Store (Data deletion section). **Vale subir esse sub-item de prioridade quando preparar o lançamento Beta.**

### 12.9 Testes

- `AuthenticationViewModelTest`: estender com cenários de reset/update password/update email (sucesso, falha de rede, `requires-recent-login`).
- Cobertura mínima: cooldown do reset (não inundar o serviço), tratamento de `requires-recent-login` virando estado `RequiresReauth`.

### 12.10 Edge cases relevantes

- Após `verifyBeforeUpdateEmail`, o e-mail antigo continua sendo o "atual" até o clique no link. UI tem que deixar claro.
- `sendPasswordResetEmail` com *enumeration protection* ativa: o Firebase pode retornar sucesso mesmo para e-mail inexistente. Mensagem ao usuário deve ser sempre genérica ("Se a conta existir, enviaremos um e-mail").
- Reauth com Google pode falhar se o token expirou — relançar Sign-In intent.
- Após `updatePassword`/`verifyBeforeUpdateEmail`/`delete`, considerar se faz sentido deslogar o usuário (sim para `delete`; recomendado para `updatePassword` para revogar outras sessões; opcional para o resto).

