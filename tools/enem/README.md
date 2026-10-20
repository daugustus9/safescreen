# Ferramentas do banco de questões do ENEM / ENEM question-bank tools

**Português.** Scripts para adicionar questões reais do ENEM em `app/src/main/assets/questions/` com segurança.

1. Baixe a prova e o gabarito em <https://www.gov.br/inep> (provas e gabaritos).
2. Pegue só a tabela de itens dos microdados, sem baixar o arquivo inteiro (cerca de 550 MB):
   `python -I fetch_zip_member.py https://download.inep.gov.br/microdados/microdados_enem_2023.zip saida ITENS_PROVA`
3. Descubra o `CO_PROVA` do caderno: é o que tem o mesmo gabarito da prova baixada (o caderno 5 amarelo do 2º dia de 2023 é o 1212).
4. Escreva as questões no JSON (veja `enem_2023_mt.json`). Use só questões que se entendem sem figura, ou declare a adaptação no campo `adaptation`.
5. Confira tudo:
   `pdftotext -enc UTF-8 prova.pdf prova.txt`
   `python -I verify_bank.py banco.json ITENS_PROVA_2023.csv 1212 prova.txt`
   O script falha se gabarito, habilidade, código do item ou dificuldade não baterem com os dados oficiais, e lista os trechos de texto que precisam de conferência manual.
6. Toda explicação precisa da revisão de um professor antes de sair da marcação "em revisão".

**English.** Scripts for safely adding real ENEM questions to `app/src/main/assets/questions/`: download the exam and answer key from INEP, extract only the item table from the microdata with `fetch_zip_member.py`, find the booklet's `CO_PROVA` by matching its answer key, write the questions as JSON, then run `verify_bank.py`. It fails when an answer, skill, item code or difficulty differs from INEP's data, and lists text that needs a manual check. Every explanation needs a teacher's review.
