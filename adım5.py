import faiss
import numpy as np
import os

os.makedirs("index", exist_ok=True)

if __name__ == "__main__":
    embeddings = np.load("data/embeddings.npy")

    dim = embeddings.shape[1]
    index = faiss.IndexFlatL2(dim)
    index.add(embeddings)

    faiss.write_index(index, "index/nutuk.index")

    print("FAISS index oluşturuldu → index/nutuk.index")
