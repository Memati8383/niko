import os
import subprocess
import requests
import faiss
import numpy as np
from pypdf import PdfReader
from sentence_transformers import SentenceTransformer

# -----------------------------
# AYARLAR
# -----------------------------
PDF_PATH = "nutuk.pdf"
INDEX_PATH = "nutuk.index"
CHUNKS_PATH = "nutuk_chunks.npy"

LLM_MODEL = "phi3"
EMBED_MODEL = "paraphrase-multilingual-MiniLM-L12-v2"

# -----------------------------
# OLLAMA MODEL KONTROL
# -----------------------------
def ensure_llm():
    try:
        r = requests.get("http://localhost:11434/api/tags", timeout=3)
        models = [m["name"] for m in r.json().get("models", [])]
        if LLM_MODEL not in models:
            print(f"🧠 Model indiriliyor: {LLM_MODEL}")
            subprocess.run(["ollama", "pull", LLM_MODEL], check=True)
        else:
            print("✅ LLM hazır")
    except:
        raise RuntimeError("❌ Ollama çalışmıyor. Ollama'yı aç.")

# -----------------------------
# PDF OKU + SPLIT
# -----------------------------
def load_chunks():
    reader = PdfReader(PDF_PATH)
    chunks = []

    def split(text, size=800, overlap=100):
        i = 0
        out = []
        while i < len(text):
            out.append(text[i:i+size])
            i += size - overlap
        return out

    for page in reader.pages:
        text = page.extract_text()
        if text:
            chunks.extend(split(text))

    return chunks

# -----------------------------
# INDEX OLUŞTUR
# -----------------------------
def build_index():
    print("📘 Nutuk okunuyor ve index oluşturuluyor...")
    chunks = load_chunks()
    np.save(CHUNKS_PATH, chunks)

    embedder = SentenceTransformer(EMBED_MODEL)
    embeddings = embedder.encode(chunks, show_progress_bar=True)

    index = faiss.IndexFlatL2(embeddings.shape[1])
    index.add(np.array(embeddings))

    faiss.write_index(index, INDEX_PATH)
    print("✅ Index hazır")

# -----------------------------
# SORU SOR
# -----------------------------
def ask(question, embedder, index, chunks):
    q_emb = embedder.encode([question])
    _, ids = index.search(q_emb, 4)

    context = "\n".join(chunks[i] for i in ids[0])

    prompt = f"""
Senin adın Niko.

Sen Mustafa Kemal Atatürk'ün Nutuk adlı eseri konusunda uzmansın.
Cevaplarını SADECE aşağıdaki Nutuk metnine dayanarak ver.

Kurallar:
- Nutuk dışında bilgi ekleme
- Tahmin yapma
- Metinde yoksa aynen şunu söyle:
"Niko olarak bu bilgiye Nutuk içerisinde rastlamadım."

Nutuk Metni:
{context}

Soru:
{question}

Niko'nun Yanıtı:
"""

    r = requests.post(
        "http://localhost:11434/api/generate",
        json={
            "model": LLM_MODEL,
            "prompt": prompt,
            "stream": False
        }
    )

    return r.json()["response"]

# -----------------------------
# MAIN
# -----------------------------
if __name__ == "__main__":

    if not os.path.exists(PDF_PATH):
        raise FileNotFoundError("❌ nutuk.pdf bulunamadı")

    ensure_llm()

    if not os.path.exists(INDEX_PATH):
        build_index()
    else:
        print("✅ Index mevcut, tekrar oluşturulmadı")

    chunks = np.load(CHUNKS_PATH, allow_pickle=True)
    index = faiss.read_index(INDEX_PATH)
    embedder = SentenceTransformer(EMBED_MODEL)

    print("\n🟢 Niko hazır. Çıkmak için 'exit' yaz.\n")

    while True:
        q = input("❓ Soru: ")
        if q.lower() == "exit":
            break

        print("\n🤖 Niko:", ask(q, embedder, index, chunks), "\n")
