# Como contribuir

Obrigado pelo interesse no ChartPatternTracker. O projeto é mantido por uma pessoa só, então as regras abaixo existem para que o seu tempo e o do mantenedor não sejam gastos em trabalho que não vai entrar.

## Antes de escrever código: issue primeiro

1. Veja o rumo do projeto no [`ROADMAP.md`](ROADMAP.md) e as [issues abertas](https://github.com/Rodorush/ChartPatternTracker/issues).
2. Issue com a etiqueta `help wanted` ou `good first issue` e **sem responsável** está livre. Comente nela dizendo que quer pegar e espere ser atribuída a você.
3. Ideia que não tem issue: abra uma (bug ou sugestão) e espere a resposta antes de implementar.

**Pull Request sem issue combinada pode ser recusado**, mesmo com o código correto: o item pode já estar em andamento ou fora do rumo do projeto.

### Áreas reservadas ao mantenedor

Não aceitam contribuição externa:

- **Monetização** (anúncios, assinatura, marketplace): itens marcados como reservados no `ROADMAP.md`.
- **Segurança do backend** (regras do Firestore e do Storage, autenticação da Cloud Function, App Check). Achou uma falha? Siga o [`SECURITY.md`](SECURITY.md), não abra issue.
- **Release**: assinatura, versão e publicação na Google Play.

## Fluxo de contribuição

1. Faça um **fork** do repositório e crie uma branch a partir de `main` (`feat/nome-curto`, `fix/nome-curto`).
2. Implemente, com testes.
3. Rode `./gradlew test`.
4. Abra um **Pull Request** contra `main`, preenchendo o modelo e citando a issue (`Closes #123`).
5. O mantenedor revisa, pode pedir ajustes e é quem faz o merge.

## Ambiente

O passo a passo está em [Configuração e instalação](README.md#configuracao-e-instalacao) no README. O essencial para quem vem de fora:

- O backend oficial **não é compartilhado**. Crie um projeto Firebase seu (Authentication, Firestore, Storage, Functions e App Check) e baixe o seu `app/google-services.json`.
- Crie o seu token em [brapi.dev](https://brapi.dev) e preencha o `local.properties`.
- Nenhum desses arquivos é versionado. **Nunca** inclua token, `google-services.json`, keystore ou `local.properties` em commit, issue ou PR.

## Convenções

As regras de código do repositório (commits em PT-BR no padrão Conventional Commits, strings nos três locales, KDoc, logs, o que não fazer) estão no [`CLAUDE.md`](CLAUDE.md), nas seções "Convenções deste repositório", "Faça" e "Não faça". Elas valem igualmente para pessoas e para agentes de IA. O trecho sobre o quadro do Trello é rotina do mantenedor e não se aplica a quem contribui.

Em resumo, um PR pronto para revisão:

- resolve **uma** issue, sem mudanças não relacionadas;
- passa em `./gradlew test` e traz teste para o que mudou (detector novo: caso positivo, negativo limítrofe e boundary em `PatternDetectionTest.kt`);
- traz toda string de interface em pt-BR, en-US e es-ES;
- atualiza a documentação que descreve o que foi alterado.

Código gerado com ajuda de IA é aceito, desde que você o tenha lido, testado e consiga explicá-lo na revisão.

## Licença das contribuições

O projeto é licenciado sob a [Apache License 2.0](LICENSE). Ao enviar uma contribuição, você concorda que ela é licenciada nos mesmos termos (seção 5 da licença) e declara ter o direito de enviá-la. Não há CLA a assinar.

## Conduta

A participação segue o [`CODE_OF_CONDUCT.md`](CODE_OF_CONDUCT.md).
