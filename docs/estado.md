# Estado do projeto

Retrato de 07/11/2026. Andamento, pontos e horas ficam no [quadro do Trello](https://trello.com/b/7RGMFlGW), que é privado.

## No ar

- **App:** fora da Play Store. O código está em `versionName 1.0.0` (`versionCode 100`), com `targetSdk 35`, que a Play recusa, porque a exigência atual é 36.
- **Site:** Política de Privacidade e Termos de Uso em `https://chartpatterntracker.web.app` (`/privacy`, `/terms` e âncora `#data-deletion`).
- **Backend:** Cloud Function `updateCandlesticks` e Firestore. A implantação e o funcionamento atuais não foram verificados, e o Ciclo 1 confere.

## Ciclo atual

**Nenhum aberto.** O C0, quadro no Trello, está fechado, com registro em [`docs/sprints/ciclo-0.md`](sprints/ciclo-0.md). O C1 abre com um plano curto e o OK do usuário.

## Próximo passo

**C1, bancada e diagnóstico:** baixar o `app/google-services.json`, instalar a Firebase CLI e fazer `test` e `assembleDebug` passarem nesta máquina. Sem o `google-services.json`, o build não fecha.

## Para publicar

O caminho tem 9 ciclos, de C0 a C8, cada um com seu cartão na lista `Ciclos` do quadro. Os bloqueios mais graves:

- `targetSdk 35`, recusado pela Play.
- Token da Brapi dentro do APK.
- Cloud Function sem exigir login, sem validar entrada e sem como ser reimplantada a partir do repositório.
- Conta pessoal na Play: o teste fechado exige 12 testadores por 14 dias seguidos.
