from sentence_transformers import SentenceTransformer
import os

MODEL_NAME = "all-MiniLM-L6-v2"
MODEL_DIR = "models/all-MiniLM-L6-v2"

os.makedirs("models", exist_ok=True)

print("Model indiriliyor...")
model = SentenceTransformer(MODEL_NAME)
model.save(MODEL_DIR)

print("Model indirildi ve kaydedildi:", MODEL_DIR)
