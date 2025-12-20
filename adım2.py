from pypdf import PdfReader

def load_pdf_text(pdf_path: str) -> str:
    reader = PdfReader(pdf_path)
    text = ""

    for page in reader.pages:
        page_text = page.extract_text()
        if page_text:
            text += page_text + "\n"

    return text


if __name__ == "__main__":
    pdf_path = "data/nutuk.pdf"
    text = load_pdf_text(pdf_path)

    with open("data/nutuk.txt", "w", encoding="utf-8") as f:
        f.write(text)

    print("PDF parse edildi → data/nutuk.txt")
