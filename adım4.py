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

    embeddings = model.encode(
        chunks,
        show_progress_bar=True,
        convert_to_numpy=True
    )

    np.save("data/embeddings.npy", embeddings)
    print("Embedding oluşturuldu → data/embeddings.npy")
