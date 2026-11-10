# Estado do projeto

Retrato de 10/11/2026. Andamento, pontos e horas ficam no [quadro do Trello](https://trello.com/b/7RGMFlGW), que é privado.

## No ar

- **App:** fora da Play Store. O código está em `versionName 1.0.0` (`versionCode 100`), com `targetSdk 35`, que a Play recusa, porque a exigência atual é 36.
- **Site:** Política de Privacidade e Termos de Uso em `https://chartpatterntracker.web.app` (`/privacy`, `/terms` e âncora `#data-deletion`).
- **Backend:** projeto Firebase `chartpatterntracker`, no plano Blaze.
  - **Cloud Function:** a `updateCandlesticks` (callable, 2ª geração, `nodejs22`, `us-central1`) está ativa, com o código de `functions/updateCandlesticks.js`. O resto do pacote implantado (`index.js`, `package.json`, lock) não está no repositório, nem as regras do Firestore e do Storage. Não houve chamadas nos últimos 30 dias.
  - **Regras:** exigem login para ler e bloqueiam a escrita, exceto em `assets/{ticker}`, que qualquer usuário logado altera.
  - **Padrões:** `candlestick_patterns` tem os 10 padrões do `PatternDetectorRegistry`.

## Ciclo atual

**C1, bancada e diagnóstico**, aberto, com 9 cartões e 16 pontos (etiqueta `C1 Bancada`). O build fecha nesta máquina: `test` (48 testes) e `assembleDebug` passam com AGP 9.5.1 e Gradle 9.6.0. No `local.properties`, `BRAPI_TOKEN`, `WEB_CLIENT_ID` e `FIREBASE_APP_CHECK_DEBUG_TOKEN` ainda estão como `REPLACE_ME`. Isso basta para compilar, mas a Brapi recusa o token. Ainda falta ver o app rodando num aparelho. Os registros dos ciclos fechados estão em [`docs/sprints/`](sprints/).

## Próximo passo

Gerar um token da Brapi e preencher as chaves do `local.properties`. Ter um aparelho ou emulador aparecendo no `adb devices` e percorrer o fluxo login → varredura → gráfico. Sem um token válido, a Brapi só responde para os tickers que libera, como PETR4 e VALE3.

## Para publicar

O caminho tem 9 ciclos, de C0 a C8, cada um com seu cartão na lista `Ciclos` do quadro. Os bloqueios mais graves:

- `targetSdk 35`, recusado pela Play.
- Token da Brapi dentro do APK.
- Cloud Function sem exigir login, sem validar entrada e sem como ser reimplantada a partir do repositório. Ela também grava o token da Brapi no log, inteiro quando ele tem até 10 caracteres.
- App Check sem enforcement no Firestore, no Storage e no Auth.
- Regras do Firestore que deixam qualquer usuário logado alterar `assets/{ticker}`.
- Conta pessoal na Play: o teste fechado exige 12 testadores por 14 dias seguidos.
