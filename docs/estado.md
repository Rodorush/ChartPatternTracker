# Estado do projeto

Retrato de 10/11/2026. Andamento, pontos e horas ficam no [quadro do Trello](https://trello.com/b/7RGMFlGW), que é privado.

## No ar

- **App:** fora da Play Store. O código está em `versionName 1.0.0` (`versionCode 100`), com `targetSdk 35`, que a Play recusa, porque a exigência atual é 36.
- **Site:** Política de Privacidade e Termos de Uso em `https://chartpatterntracker.web.app` (`/privacy`, `/terms` e âncora `#data-deletion`).
- **Backend:** Cloud Function `updateCandlesticks` e Firestore. A implantação e o funcionamento atuais não foram verificados, e o Ciclo 1 confere.

## Ciclo atual

**C1, bancada e diagnóstico**, aberto, com 7 cartões e 14 pontos (etiqueta `C1 Bancada`). O build fecha nesta máquina: `test` (48 testes) e `assembleDebug` passam com AGP 9.5.1 e Gradle 9.6.0. Ainda falta ver o app rodando num aparelho e ter um retrato real do backend. Os registros dos ciclos fechados estão em [`docs/sprints/`](sprints/).

## Próximo passo

Ter um aparelho ou emulador aparecendo no `adb devices` e percorrer o fluxo login → varredura → gráfico. O diagnóstico do backend já pode começar, porque a Firebase CLI está instalada e logada na conta dona do `chartpatterntracker`.

## Para publicar

O caminho tem 9 ciclos, de C0 a C8, cada um com seu cartão na lista `Ciclos` do quadro. Os bloqueios mais graves:

- `targetSdk 35`, recusado pela Play.
- Token da Brapi dentro do APK.
- Cloud Function sem exigir login, sem validar entrada e sem como ser reimplantada a partir do repositório.
- Conta pessoal na Play: o teste fechado exige 12 testadores por 14 dias seguidos.
