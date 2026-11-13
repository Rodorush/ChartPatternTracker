# Política de segurança

## Como relatar uma vulnerabilidade

**Não abra issue pública nem pull request** para falha de segurança: o relato ficaria visível antes da correção.

Use o relato privado do GitHub: aba **Security** do repositório → **Report a vulnerability** ([link direto](https://github.com/Rodorush/ChartPatternTracker/security/advisories/new)). Se não for possível, escreva para o e-mail de contato indicado na seção [Autor](README.md#autor) do README.

Inclua, quando puder:

- o que é afetado (app, Cloud Function, regras do Firestore ou do Storage, site);
- os passos para reproduzir;
- o impacto que você enxerga.

O projeto é mantido por uma pessoa só. A intenção é responder em até 7 dias e avisar quando a correção for publicada.

## Escopo

- O código deste repositório e o backend oficial do aplicativo (projeto Firebase `chartpatterntracker`).
- Ficam fora: serviços de terceiros (Brapi, Firebase, Google Play) e instalações feitas a partir de forks.

## Ao testar

Use apenas a sua própria conta, não acesse dados de outras pessoas e não execute teste que degrade o serviço ou gere custo, como chamadas em massa à Cloud Function.

## Versões atendidas

Somente a versão mais recente da branch `main`.
