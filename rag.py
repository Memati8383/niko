from fastapi import FastAPI
import faiss
import numpy as np
import requests
from sentence_transformers import SentenceTransformer

app = FastAPI()

index = faiss.read_index("nutuk.index")
chunks = np.load("nutuk_chunks.npy", allow_pickle=True)
embedder = SentenceTransformer("paraphrase-multilingual-MiniLM-L12-v2")

@app.get("/ask")
def ask(q: str):
    q_emb = embedder.encode([q])
    _, ids = index.search(q_emb, 4)

    context = "\n".join(chunks[i] for i in ids[0])

    prompt = f"""
Senin adın Niko.
Cevaplarını SADECE Nutuk metnine dayanarak ver.
Metinde yoksa: "Niko olarak bu bilgiye Nutuk içerisinde rastlamadım."

Nutuk Metni:
{context}

Soru:
{q}

Niko'nun Yanıtı:
"""

    r = requests.post(
        "http://localhost:11434/api/generate",
        json={"model": "phi3", "prompt": prompt, "stream": False}
    )

    return {"answer": r.json()["response"]}
