import faiss
import numpy as np
from sentence_transformers import SentenceTransformer

MODEL_DIR = "models/all-MiniLM-L6-v2"

def load_chunks():
    with open("data/chunks.txt", "r", encoding="utf-8") as f:
        content = f.read()
    return [c.strip() for c in content.split("---") if c.strip()]


if __name__ == "__main__":
    chunks = load_chunks()

    model = SentenceTransformer(MODEL_DIR)
    index = faiss.read_index("index/nutuk.index")

    query = "Egemenlik nedir?"
    query_embedding = model.encode([query], convert_to_numpy=True)

    distances, indices = index.search(query_embedding, 5)

    print("Soru:", query)
    print("\nEn alakalı parçalar:\n")

    for i in indices[0]:
        print("----")
        print(chunks[i][:500])
