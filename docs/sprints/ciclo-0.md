# Ciclo 0: quadro no Trello

Registro de execução. Quadro: https://trello.com/b/7RGMFlGW, etiqueta `C0 Quadro`. Cartão do ciclo: https://trello.com/c/H9JnR4dk

## Resultado

| Início | Fim | Cartões | Pontos | Horas do usuário | Horas por ponto |
|---|---|---|---|---|---|
| 07/11/2026 | 07/11/2026 | 5/5 | 8/8 | 0,75 | 0,09 |

Pontos por semana: não se aplica, porque o ciclo durou menos de 1 dia corrido no quadro.

## Cartões

| Cartão | Quem | Pts | Horas | Concluído em |
|---|---|---|---|---|
| [Gerar chave e token do Trello](https://trello.com/c/m7Ta1R3A) | Rodolfo | 1 | 0,5 | 07/11 08:01 |
| [Escrever a skill e o script do Trello](https://trello.com/c/yofdTmFT) | Claude | 3 | - | 07/11 08:01 |
| [Criar o quadro privado e a carga inicial](https://trello.com/c/aiOEQqw1) | Claude | 2 | - | 07/11 08:02 |
| [Validar histórico e métricas com um cartão de teste](https://trello.com/c/NQdALA97) | Claude | 1 | - | 07/11 08:02 |
| [Registrar o quadro no repositório](https://trello.com/c/IHE7c2a4) | Claude | 1 | 0,25 | 07/11 09:15 |

Horários no fuso de Brasília (UTC−3).

## Como ler estes números

- Quatro cartões não têm tempo de ciclo. O trabalho deles aconteceu antes de o quadro existir, e eles foram direto de `Ciclo atual` para `Em teste`. Só o último passou por `Em andamento`, com 1,4 h até o aceite.
- As horas são só as do usuário. O tempo do Claude não é medido, então os cartões dele aparecem sem horas, e as horas por ponto do ciclo ficam baixas.
- Nenhuma estimativa de pontos mudou durante o ciclo.

## Achados

- O Trello grava `createCard` e `updateCard` com `listBefore` e `listAfter`, e o horário registrado fica a menos de 0,4 s do relógio local. Isso confirma a suposição do plano sobre o histórico.
- A primeira credencial gerada foi um token da conta Atlassian (`ATATT...`), que não serve na API do Trello. A skill `trello` agora explica como reconhecer o formato.
- O script tinha quatro defeitos de exibição nas métricas, todos corrigidos com teste:
  - pontos por semana com o ciclo aberto ou com menos de 1 dia;
  - horas por ponto mostrando `0` sem horas registradas;
  - horas por ponto contando cartões não concluídos;
  - horas arredondadas para uma casa, com `0,25` aparecendo como `0,2`.

## Arquivos

- **Repositório**, commit `11cc3c8`:
  - `CLAUDE.md`: seção "Quadro do projeto";
  - `ROADMAP.md`: o `[~]` deixa de ser usado;
  - `docs/estado.md`: novo.
- **Fora do repositório:** skill `~/.claude/skills/trello/`, com 22 testes offline.
