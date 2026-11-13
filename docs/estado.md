# Estado do projeto

Retrato de 13/11/2026. Andamento, pontos e horas ficam no [quadro do Trello](https://trello.com/b/7RGMFlGW), que é privado.

## No ar

- **App:** fora da Play Store. O código está em `versionName 1.0.0` (`versionCode 100`), com `targetSdk 35`, que a Play recusa, porque a exigência atual é 36.
- **Site:** Política de Privacidade e Termos de Uso em `https://chartpatterntracker.web.app` (`/privacy`, `/terms` e âncora `#data-deletion`).
- **Backend:** projeto Firebase `chartpatterntracker`, no plano Blaze.
  - **Cloud Function:** a `updateCandlesticks` (callable, 2ª geração, `nodejs22`, `us-central1`) está ativa, com o código de `functions/`, que é cópia do pacote implantado. Não houve chamadas nos últimos 30 dias.
  - **Regras:** `firestore.rules` e `storage.rules` são cópia do que está implantado. Exigem login para ler e bloqueiam a escrita, exceto em `assets/{ticker}`, que qualquer usuário logado altera.
  - **Padrões:** `candlestick_patterns` tem os 10 padrões do `PatternDetectorRegistry`.

## Ciclo atual

**C1, bancada e diagnóstico**, aberto, com 25 cartões e 35 pontos (etiqueta `C1 Bancada`). O build fecha nesta máquina: `test` (50 testes) e `assembleDebug` passam com AGP 9.5.1 e Gradle 9.6.0. No `local.properties`, `BRAPI_TOKEN` e `WEB_CLIENT_ID` estão preenchidos: a Brapi responde 200 para WEGE3, e o `WEB_CLIENT_ID` é o client OAuth do tipo Web do `google-services.json`. O login com Google só funciona com o SHA-1 da chave de debug da máquina registrado no app Android do projeto Firebase (`firebase apps:android:sha:create`) e o `google-services.json` baixado depois disso; o desta máquina está registrado. O `FIREBASE_APP_CHECK_DEBUG_TOKEN` segue `REPLACE_ME`, porque o App Check está sem enforcement. Os testes rodam no aparelho físico, um Galaxy S26 FE (1080x2340, 450 dpi, Android 17), pelo cabo: ele aparece no `adb devices` com a Depuração USB ligada e o Bloqueador automático desligado. Não há emulador, porque o PC não o sustenta. O app roda no aparelho: login com Google, aviso de risco, varredura no Diário, gráfico e exclusão de conta funcionam. O plano do token da Brapi só entrega o intervalo `1d` (medido em 12/11/2026): nos outros 8 tempos da lista, a Brapi responde 400 e a tela de resultados avisa quais pares de ativo e tempo ficaram sem dados. Os defeitos do percurso são cartões de 1 ou 2 pontos no quadro. Os registros dos ciclos fechados estão em [`docs/sprints/`](sprints/).

## Próximo passo

Seguir pelos defeitos do percurso em `Ciclo atual`, começando por não cancelar a varredura ao abrir o gráfico (`7hIrPwOO`). O token da Brapi em uso é de bancada: a Cloud Function o grava no log, então ele é revogado no fim do caminho.

## Para publicar

O caminho tem 9 ciclos, de C0 a C8, cada um com seu cartão na lista `Ciclos` do quadro. Os bloqueios mais graves:

- `targetSdk 35`, recusado pela Play.
- Token da Brapi dentro do APK.
- Cloud Function sem exigir login e sem validar entrada. A reimplantação a partir do repositório ainda não foi testada, e as variáveis de ambiente da função implantada não foram conferidas. Ela também grava o token da Brapi no log a cada chamada, porque registra o payload inteiro que o app envia.
- App Check sem enforcement no Firestore, no Storage e no Auth.
- Regras do Firestore que deixam qualquer usuário logado alterar `assets/{ticker}`.
- Conta pessoal na Play: o teste fechado exige 12 testadores por 14 dias seguidos.
