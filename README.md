[English](README.en.md) · **Português**

# SafeScreen (protótipo)

App Android de código aberto que torna o tempo de tela das crianças mais seguro e mais produtivo. Este é o protótipo do MVP do plano do projeto; os códigos de funcionalidade abaixo (S1, C2…) se referem a esse plano.

O idioma principal do app é o português do Brasil; o inglês é usado em celulares configurados em inglês.

O protótipo **não tem permissão de INTERNET**: nada do que ele vê pode sair do celular. Os controles dos pais ficam protegidos por um PIN no mesmo celular; o envio criptografado para o celular dos pais é o próximo passo (veja *Fora do protótipo*).

## O que tem

| Código | Funcionalidade | Onde |
|---|---|---|
| S1 | Detecção de risco no próprio celular (automutilação, desafios perigosos, drogas, armas) no texto digitado, em português e inglês | `core/RiskClassifier.kt`, regras em `assets/rules/risk_rules.json`, executadas por `service/GuardAccessibilityService.kt` |
| S5 | Botão "Preciso de ajuda": adulto de confiança e linhas de ajuda (CVV 188, Disque 100…); só abre o discador | `ui/HelpScreen.kt`, `assets/content/helplines.json` |
| S7, W1 | Cartões de segurança e de conselhos para a vida, por idade | `assets/content/cards.json`, `core/CardScheduler.kt` |
| — | Cartões de incentivo: autoestima, hábitos, gentileza, cidadania; a família pode escrever os seus | `assets/content/cards.json`, `ui/ParentScreen.kt` |
| W3 | Cartões da hora de dormir no horário combinado | `core/CardScheduler.kt` |
| P3 | Tempo de foco; os apps escolhidos voltam para a tela inicial enquanto ele roda | `ui/ChildHomeScreen.kt`, `service/GuardAccessibilityService.kt` |
| C1 | Lembretes da escola: na véspera (a partir das 17h) e na manhã do dia | `core/CardScheduler.kt` |
| C2 | Lê as notificações dos apps da escola escolhidos pela família (Agenda Edu, Escola Paraná…) e as transforma em tarefas | `core/SchoolNoticeParser.kt`, `service/SchoolNotificationListener.kt` |
| T1, C3 | Resumo semanal: categorias de alerta, pedidos de ajuda, escola, foco, tempo de tela; detalhe por app só para menores de 10 anos | `core/WeeklySummary.kt`, `ui/ParentScreen.kt` |
| T2 | Acordo da família; qualquer mudança de regra pede nova assinatura dos pais e da criança | `data/PolicyStore.kt`, `ui/ParentScreen.kt` |
| T5 | Alerta imediato para S1 e S5 (no protótipo, uma notificação no próprio celular, que a criança também vê) | `system/AlertNotifier.kt` |
| T6 | Faixas etárias: menos de 10, 10 a 12, 13+ | `core/Models.kt` |
| — | Tela "O que o SafeScreen vê", mostrada antes de qualquer coisa ser ligada e acessível pela notificação de status | `ui/TransparencyScreen.kt` |
| — | Tela "Todas as mensagens": a criança e os pais podem ler todas as mensagens que o app pode mostrar, inclusive as da família | `ui/MessagesScreen.kt` |
| P4 | Questões reais do ENEM (13+): convite uma vez por dia, explicação depois da resposta, da mais fácil para a mais difícil, e erros que voltam depois | `core/Study.kt`, `ui/QuestionScreen.kt`, `assets/questions/` |
| P4 | Desafio do Dia: as mesmas 3 questões para todos no dia, escolhidas só pela data; o resultado é compartilhado como quadradinhos 🟩🟥, sem respostas nem dados pessoais; sequência com um dia de folga por semana | `core/Game.kt`, `ui/DailyChallengeScreen.kt` |
| P4 | Álbum de Habilidades: uma figurinha por habilidade do ENEM (120); acertar cola, acertar questão difícil deixa brilhante; figurinhas faltando podem ser praticadas ou pedidas a um amigo | `core/Game.kt`, `ui/AlbumScreen.kt` |
| P4 | "Onde focar" no resumo dos pais: habilidades do ENEM a revisar, pesando mais os erros em questões fáceis (dificuldade oficial do INEP) | `core/Study.kt`, `ui/ParentScreen.kt` |

O motor de mensagens (`CardOverlayService` + `CardPresenter`) foi adaptado do serviço de sobreposição do app de mensagens subliminares, com uma mudança proposital: toda mensagem é visível e fica na tela até a criança ler. Nada é subliminar. Segurança, conselhos e incentivo se revezam, para que nunca apareçam três avisos seguidos.

## Privacidade desde a construção

- O texto digitado é verificado na memória e descartado; só a categoria ("SELF_HARM") é registrada. Campos de senha são ignorados.
- O serviço de acessibilidade escuta apenas dois tipos de evento (texto digitado, app em primeiro plano) e não consegue ler o conteúdo da tela (`canRetrieveWindowContent="false"`).
- Notificações de apps que não são os apps da escola escolhidos são ignoradas sem serem lidas.
- Os eventos ficam 30 dias no celular (`data/EventLog.kt`); backup na nuvem e transferência entre aparelhos estão desativados.
- Todos os cartões e todas as regras de detecção são arquivos de texto simples em `app/src/main/assets`.

## Como compilar

Requer JDK 17+ e o Android SDK (plataforma 36). Aponte `JAVA_HOME` para o JDK e rode:

```bash
./gradlew testDebugUnitTest assembleDebug
```

O APK é gerado em `app/build/outputs/apk/debug/app-debug.apk`. Os testes de unidade cobrem o classificador (com o arquivo de regras real), o leitor de avisos da escola, o agendador de cartões, o resumo semanal e a biblioteca de mensagens.

## Como testar no celular

1. Instale: `adb install -r app/build/outputs/apk/debug/app-debug.apk`
2. Leia a tela de transparência e ligue cada acesso em **Configurar**. No Android 13+, um app instalado fora da Play Store precisa de *Configurações › Apps › SafeScreen › ⋮ › Permitir configurações restritas* antes de ligar a acessibilidade e o acesso às notificações.
3. Toque em **Ligar** e depois em **Mostrar um cartão agora**.
4. Detecção de risco: digite `desafio do desmaio` ou `quero morrer` na busca de qualquer app. Aparece um cartão de ajuda e uma notificação de alerta de segurança. O resumo dos pais mostra só a categoria.
5. Avisos da escola: em **Área dos pais › Acordo › Apps da escola**, escolha um app de mensagens e mande para você mesmo uma notificação como `Prova de matemática amanhã`. Ou cole o texto em **Todas as tarefas**.
6. Foco: escolha um app em *Apps pausados durante o foco*, comece uma sessão de foco e abra esse app.

## Banco de questões

São 80 questões do ENEM 2023 (cadernos amarelos), escolhidas porque se entendem sem figura: 18 de Linguagens, 21 de Ciências Humanas, 25 de Ciências da Natureza e 16 de Matemática. Ficaram de fora as baseadas em imagens, letras de música ou poemas inteiros, e três textos sensíveis são só para 15 anos ou mais. Gabarito, habilidade (H1 a H30 de cada área) e dificuldade vêm dos microdados oficiais do INEP, e o texto foi conferido com a prova. As áreas se revezam, e a dificuldade (mais fácil, média, difícil) é relativa a cada área. As explicações foram escritas pelo SafeScreen e **ainda precisam de revisão por professores**. Para adicionar questões, veja [tools/enem](tools/enem/README.md).

## Fora do protótipo

- Pareamento com o celular dos pais e envio com criptografia de ponta a ponta (alertas e resumos no celular dos pais).
- S2 detecção de aliciamento e W7 avisos de gentileza (v2: precisam de um modelo treinado no próprio celular).
- Integração com o Google Classroom (P1) e a visão do professor (C6–C8).
- Questões para menores de 13 anos (OBMEP e questões baseadas na BNCC), outras áreas do ENEM e vestibulares (com autorização das universidades).
- iOS.
- Revisão das políticas da Play Store: o uso do serviço de acessibilidade precisa ser declarado e justificado antes da publicação.

## Licença

- **Código:** [GNU General Public License v3.0](LICENSE). Qualquer pessoa pode usar, estudar e modificar; versões distribuídas precisam continuar abertas sob a mesma licença.
- **Biblioteca de mensagens e regras de detecção** (`app/src/main/assets/content/` e `app/src/main/assets/rules/`): [Creative Commons Atribuição-CompartilhaIgual 4.0](LICENSES/CC-BY-SA-4.0.txt).
- **Questões do ENEM** (`app/src/main/assets/questions/enem_*.json`): enunciados e alternativas são do INEP, reproduzidos para fins de estudo com indicação da fonte, e não estão cobertos pelas licenças acima. As explicações e dicas de estudo são do SafeScreen, sob CC BY-SA 4.0.
- O nome "SafeScreen" e o logotipo não estão cobertos por essas licenças.

Copyright © 2026 Denis Augusto.
