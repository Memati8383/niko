def chunk_text(text, chunk_size=500, overlap=100):
    chunks = []
    start = 0

    while start < len(text):
        end = start + chunk_size
        chunks.append(text[start:end])
        start += chunk_size - overlap

    return chunks


if __name__ == "__main__":
    with open("data/nutuk.txt", "r", encoding="utf-8") as f:
        text = f.read()

    chunks = chunk_text(text)

    with open("data/chunks.txt", "w", encoding="utf-8") as f:
        for chunk in chunks:
            f.write(chunk.replace("\n", " ") + "\n---\n")

    print(f"Chunk sayısı: {len(chunks)}")
