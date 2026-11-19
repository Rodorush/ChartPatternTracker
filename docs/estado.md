# Estado do projeto

Retrato de 19/11/2026. Andamento, pontos e horas ficam no [quadro do Trello](https://trello.com/b/7RGMFlGW), que é privado.

## No ar

- **App:** fora da Play Store. O código está em `versionName 1.0.0` (`versionCode 100`), com `targetSdk 35`, que a Play recusa, porque a exigência atual é 36.
- **Site:** Política de Privacidade e Termos de Uso em `https://chartpatterntracker.web.app` (`/privacy`, `/terms` e âncora `#data-deletion`).
- **Backend:** projeto Firebase `chartpatterntracker`, no plano Blaze.
  - **Cloud Function:** a `updateCandlesticks` (callable, 2ª geração, `nodejs22`, `us-central1`) roda o código de `functions/`. Só atende usuário logado, valida os parâmetros da chamada, roda com no máximo 5 instâncias, limita cada usuário a 300 chamadas por dia e não grava o token da Brapi no log. A recusa da Brapi por plano volta ao app como `failed-precondition`, e o app a trata como falha esperada do par, fora do Crashlytics. Até ser fechado, o app não chama mais a função para o par recusado, para não gastar cota. Também não a chama para o par que já tem a vela do último pregão, lida depois do último fechamento (19:00 do pregão): fim de semana, feriado e o horário antes da abertura da B3 não pedem vela nova. A série lida com o pregão aberto é buscada mais uma vez depois do fechamento, e a vela fechada substitui a parcial no Room. Para isso, a função grava em cada vela o `fetchedAt`, o instante da consulta à Brapi.
  - **Regras:** `firestore.rules` e `storage.rules` são o que está implantado. Exigem login para ler e bloqueiam toda escrita do cliente; quem grava é a Cloud Function. No Firestore, o cliente lê só `candlesticks`, `candlestick_patterns` e `assets`; a cota por usuário, em `rate_limits`, fica fora do alcance dele.
  - **Padrões:** `candlestick_patterns` tem os 10 padrões do `PatternDetectorRegistry`.

## Ciclo atual

**C1, bancada e diagnóstico**, aberto, com 56 cartões e 79 pontos (etiqueta `C1 Bancada`). O build fecha nesta máquina: `test` (136 testes) e `assembleDebug` passam com AGP 9.5.1 e Gradle 9.6.0. No `local.properties`, `BRAPI_TOKEN` e `WEB_CLIENT_ID` estão preenchidos: a Brapi responde 200 para WEGE3, e o `WEB_CLIENT_ID` é o client OAuth do tipo Web do `google-services.json`. O login com Google só funciona com o SHA-1 da chave de debug da máquina registrado no app Android do projeto Firebase (`firebase apps:android:sha:create`) e o `google-services.json` baixado depois disso; o desta máquina está registrado. O `FIREBASE_APP_CHECK_DEBUG_TOKEN` segue `REPLACE_ME`. Os testes rodam no aparelho físico, um Galaxy S26 FE (1080x2340, 450 dpi, Android 17), pelo cabo: ele aparece no `adb devices` com a Depuração USB ligada e o Bloqueador automático desligado. Não há emulador, porque o PC não o sustenta. O app roda no aparelho: login com Google, entrada direta no menu com a sessão salva, Voltar no menu levando o app para segundo plano, volta ao login quando a sessão acaba fora do app, aviso de risco, varredura no Diário, gráfico e exclusão de conta funcionam. O que o plano do token da Brapi entrega depende do ativo (medido em 15/11/2026): PETR4 e VALE3 saem em `1h`, `1d` e `1wk`, e WEGE3 só em `1d`. Nos tempos recusados, a Brapi responde 400, e a tela de resultados avisa quais pares de ativo e tempo ficaram sem dados. Os defeitos do percurso são cartões de 1 ou 2 pontos no quadro. Os registros dos ciclos fechados estão em [`docs/sprints/`](sprints/).

## Próximo passo

Voltar ao fluxo normal pelos defeitos do percurso em `Ciclo atual`. Seis cartões de lá têm issue espelho aberta a contribuição (seção abaixo): antes de pegar um desses, conferir se a issue já tem responsável. Quem comentar no post de abertura pedindo para testar o Beta entra na lista do cartão de testadores (RYQjL64K), no Backlog.

## Abertura do código

O repositório é público no GitHub, sob Apache-2.0, com `CONTRIBUTING.md`, `SECURITY.md` e modelos de issue e de PR.

- **Proteções:** relato privado de vulnerabilidade, secret scanning com push protection e proteção da `main`. A `main` só recebe PR, sem aprovação obrigatória, sem force push e sem deleção, e a regra vale também para o administrador.
- **Abertas a contribuição:** as issues #80 a #84 e #87 (`good first issue`), espelhos de cartões de `Ciclo atual`, com o link no cartão.

## Para publicar

O caminho tem 9 ciclos, de C0 a C8, cada um com seu cartão na lista `Ciclos` do quadro. Os bloqueios mais graves:

- `targetSdk 35`, recusado pela Play.
- Cartões do C3 (backend seguro), no quadro.
- Conta pessoal na Play: o teste fechado exige 12 testadores por 14 dias seguidos.
