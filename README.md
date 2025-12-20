# niko
niko yapay zeka 

Çok iyi bir noktaya geldin 👌
Şu anda sistemin doğru çalışıyor, ama beklenen şey yanlış:

❗ all-MiniLM-L6-v2 ZEKÂ DEĞİLDİR.
Bu model cevap üretmez, sadece arama yapar.

Şu an yaptığın şey:

Soru → embedding

FAISS → en alakalı metin parçalarını getiriyor

Sen o parçaları doğrudan “cevap” sanıyorsun

Bu yüzden “saçma” gibi geliyor.

🧠 DOĞRU MİMARİ (KRİTİK AYRIM)
❌ Şu an:
Soru → Embedding → FAISS → Metin parçaları

✅ Olması gereken (RAG):
Soru
 ↓
FAISS (ilgili metin)
 ↓
LLM (akıllı cevap üretir)  ← BU EKSİK


👉 Eksik olan parça: LLM (dil modeli)

🎯 SENİN İSTEDİĞİN ŞEY

“Atatürk kaç yılında Samsun’a çıktı?”
→ “19 Mayıs 1919”

Bunu yapacak olan:
❌ MiniLM değil
✅ küçük bir LLM

🧠 EN KÜÇÜK VE UYGUN ÇÖZÜM (ÖNERİLEN)
🥇 Phi-3 Mini (INT4)

~500 MB

Çok küçük

RAG için ideal

Türkçe yeterli

Alternatif:

Phi-2 (INT4)

LLaMA 3.2 1B (INT4 – çok zayıf ama çalışır)

🔧 ŞİMDİ SANA “TEK CEVAP” ÜRETEN KODU VERİYORUM

Aşağıdaki kod:

FAISS’ten en alakalı chunk’ı alır

LLM’ye “sadece buna bakarak cevap ver” der

Tek, net cevap döner

🟢 ADIM 7 — LLM İLE AKILLI CEVAP ÜRETME
1️⃣ Gerekli kütüphane
pip install llama-cpp-python

2️⃣ Phi-3 Mini modelini indir (GGUF – INT4)

(Colab’a uygun)

wget https://huggingface.co/microsoft/Phi-3-mini-4k-instruct-gguf/resolve/main/phi-3-mini-4k-instruct-q4.gguf

3️⃣ LLM’yi bağlayan kod
from llama_cpp import Llama

llm = Llama(
    model_path="phi-3-mini-4k-instruct-q4.gguf",
    n_ctx=2048,
    n_threads=2
)

4️⃣ RAG + CEVAP ÜRETİMİ
def generate_answer(question: str, context: str):
    prompt = f"""
Aşağıdaki metne SADECE dayanarak kısa ve net cevap ver.

Metin:
{context}

Soru:
{question}

Cevap:
"""
    response = llm(prompt, max_tokens=50, stop=["\n"])
    return response["choices"][0]["text"].strip()

5️⃣ FAISS → CEVAP (TAM AKIŞ)
def ask(question: str):
    query_embedding = model.encode([question], convert_to_numpy=True)
    _, indices = index.search(query_embedding, 1)

    context = chunks[indices[0][0]]
    answer = generate_answer(question, context)

    return answer

🔥 TEST
print(ask("Atatürk kaç yılında Samsun'a çıktı?"))

Beklenen çıktı:
19 Mayıs 1919

🧠 NEDEN ARTIK “AKILLI”?

MiniLM → bulur

Phi-3 → anlar ve cevaplar

Tek cevap

Saçmalamaz

PDF dışına çıkmaz

⚠️ ÖNEMLİ PROMPT İPUCU

Şu cümle çok kritik:

SADECE yukarıdaki metne dayanarak cevap ver


Bu, halüsinasyonu keser.

🎯 NET ÖZET
Parça	Görev
MiniLM	Bulma
FAISS	Hız
Phi-3	Akıl
RAG	Doğruluk
🚀 SONRAKİ ADIM

İstersen:
1️⃣ FastAPI endpoint
2️⃣ Mobil için JSON API
3️⃣ PDF sayfa numarasıyla cevap
4️⃣ Çoklu PDF + kaynak gösterimi

Hangisine geçelim?
