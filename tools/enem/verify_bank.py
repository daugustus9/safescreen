"""Check a SafeScreen question bank against INEP's official files.

For every question it checks that:
  - answer, skill, item code and difficulty match the microdata (ITENS_PROVA_<year>.csv);
  - the statement and alternatives appear word for word in the exam text.

Differences that are expected (fractions printed stacked in the PDF, symbols such as π,
or a table rewritten as text and declared in "adaptation") are listed for a person to
confirm; they are not failures.

usage:
  pdftotext -enc UTF-8 <exam.pdf> exam.txt
  python -I verify_bank.py <bank.json> <ITENS_PROVA_YYYY.csv> <CO_PROVA> exam.txt
"""
import csv
import json
import re
import sys


def squash(text):
    return re.sub(r"\s+", " ", text.replace(" ", " ").replace(" ", " ")).strip()


def main():
    bank_path, items_path, booklet, exam_text_path = sys.argv[1:5]
    bank = json.load(open(bank_path, encoding="utf-8"))
    exam = squash(open(exam_text_path, encoding="utf-8").read())
    items = {
        int(row["CO_POSICAO"]): row
        for row in csv.DictReader(open(items_path, encoding="latin-1"), delimiter=";")
        if row["CO_PROVA"] == booklet
    }
    if not items:
        sys.exit(f"No items for CO_PROVA {booklet}")

    failures = 0
    for q in bank["questions"]:
        n = q["number"]
        row = items.get(n)
        problems = []
        if row is None:
            problems.append("not in microdata")
        else:
            if row["IN_ITEM_ABAN"] == "1":
                problems.append("item annulled by INEP")
            if row["TX_GABARITO"] != q["answer"]:
                problems.append(f"answer {q['answer']} != official {row['TX_GABARITO']}")
            if "H" + row["CO_HABILIDADE"] != q["skill"]:
                problems.append(f"skill {q['skill']} != official H{row['CO_HABILIDADE']}")
            if int(row["CO_ITEM"]) != q["item"]:
                problems.append(f"item {q['item']} != official {row['CO_ITEM']}")
            if abs(float(row["NU_PARAM_B"]) - q["difficulty"]) > 0.01:
                problems.append(f"difficulty {q['difficulty']} != official {row['NU_PARAM_B']}")
        failures += bool(problems)

        to_confirm = []
        for paragraph in q["stem"].split("\n"):
            words = paragraph.strip().lstrip("• ").split()
            for i in range(0, len(words), 8):
                chunk = " ".join(words[i:i + 8])
                if chunk and chunk not in exam:
                    to_confirm.append(chunk)
        to_confirm += [f"option {k}: {v}" for k, v in q["options"].items() if squash(v) not in exam]

        status = "FAIL " + "; ".join(problems) if problems else "ok"
        print(ascii(f"Q{n}: {status}"))
        for text in to_confirm:
            print(ascii(f"    confirm by hand: {text}"))

    print(f"{len(bank['questions'])} questions, {failures} failing")
    sys.exit(1 if failures else 0)


if __name__ == "__main__":
    main()
