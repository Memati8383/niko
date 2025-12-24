import json
from pypdf import PdfReader

reader = PdfReader("nutuk.pdf")
with open("nutuk_train.jsonl", "w", encoding="utf-8") as f:
    for page in reader.pages:
        text = page.extract_text()
        if len(text) > 300:
            # Alpaca formatı (Eğitim için en standart format)
            data = {
                "instruction": "Aşağıdaki Nutuk bölümünü özetle ve üslubu öğren:",
                "input": "",
                "output": text.strip()
            }
            f.write(json.dumps(data, ensure_ascii=False) + "\n")
