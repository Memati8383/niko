# niko
niko yapay zeka 

Aşağıda verdiğin sistem için temiz, anlaşılır ve profesyonel bir README.md hazırladım.
Bunu doğrudan README.md olarak kaydedip kullanabilirsin.


---

📘 Niko – Nutuk Local RAG Asistanı

Niko, Mustafa Kemal Atatürk’ün Nutuk adlı eserini temel alarak çalışan,
tamamen local, ücretsiz, kotasız bir Soru-Cevap (RAG) uygulamasıdır.

Bu proje:

❌ Bulut API kullanmaz

❌ Gemini / OpenAI kullanmaz

❌ LangChain kullanmaz

✅ Ollama + Local LLM kullanır

✅ FAISS ile vektör arama yapar

✅ Nutuk dışına çıkmaz



---

🚀 Özellikler

📘 Kaynak: nutuk.pdf

🧠 Model: phi-3 (Ollama)

🔍 Arama: FAISS (local vector database)

🇹🇷 Türkçe prompt optimizasyonu

🧠 Hallüsinasyon azaltılmış cevaplar

⚡ Index ve model sadece 1 kere oluşturulur

💻 Windows uyumlu



---

🧱 Mimari

Kullanıcı Sorusu
       ↓
Sentence-Transformers (Embedding)
       ↓
FAISS (Benzer metinleri bulur)
       ↓
Ollama (Local LLM)
       ↓
Niko'nun Yanıtı


---

🛠️ Gereksinimler

1️⃣ Ollama

Ollama’yı indirip kur:

https://ollama.com/download

Kurulumdan sonra Ollama açık olmalı.


---

2️⃣ Python

Python 3.9+ önerilir


Gerekli paketler:

pip install faiss-cpu sentence-transformers pypdf requests


---

📂 Dosya Yapısı

project/
│
├─ niko_nutuk_cli.py
├─ nutuk.pdf
├─ README.md
│
├─ nutuk.index          (otomatik oluşur)
├─ nutuk_chunks.npy    (otomatik oluşur)


---

▶️ Çalıştırma

python niko_nutuk_cli.py

İlk çalıştırmada:

phi-3 modeli otomatik indirilir

Nutuk.pdf parçalanır

FAISS index oluşturulur


Sonraki çalıştırmalar: ⚡ Çok hızlı başlar (tekrar işlem yapılmaz)


---

💬 Kullanım

Program başladıktan sonra terminalden soru sorabilirsin:

❓ Soru: Samsun'a çıkışın önemi nedir?
🤖 Niko: ...

Çıkmak için:

exit


---

📜 Cevap Kuralları

Niko:

Sadece Nutuk metnine dayanır

Tahmin yapmaz

Yorum katmaz

Nutuk’ta yoksa şu cevabı verir:


> "Niko olarak bu bilgiye Nutuk içerisinde rastlamadım."




---

🧠 Model Bilgisi

Varsayılan model:

phi3 (hafif, hızlı, 4 GB RAM yeterli)


İstersen koddan şu modellere geçebilirsin:

mistral:7b (8 GB RAM)

llama3:8b (12+ GB RAM)



---

🔒 Gizlilik

Tüm işlemler bilgisayarınızda gerçekleşir

İnternet sadece ilk model indirme için gerekir

Hiçbir veri dışarı gönderilmez



