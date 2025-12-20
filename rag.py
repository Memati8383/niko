import os
import sys
import logging

# Loglama yapılandırması
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# Gerekli paketleri kontrol et
try:
    from huggingface_hub import hf_hub_download
    from langchain_community.document_loaders import PyPDFLoader
    try:
        from langchain_text_splitters import RecursiveCharacterTextSplitter
    except ImportError:
        from langchain.text_splitter import RecursiveCharacterTextSplitter
    from langchain_community.vectorstores import Chroma
    from langchain_community.embeddings import HuggingFaceEmbeddings
    from langchain_community.llms import LlamaCpp
    from langchain.chains import RetrievalQA
    from langchain.prompts import PromptTemplate
except ImportError as e:
    logger.error(f"Gerekli paket eksik: {e}")
    logger.error("Lütfen şunu çalıştırın: pip install langchain langchain-community pypdf chromadb sentence-transformers llama-cpp-python huggingface_hub")
    sys.exit(1)

# Yapılandırma
MODEL_REPO = "microsoft/Phi-3-mini-4k-instruct-gguf"
MODEL_FILENAME = "Phi-3-mini-4k-instruct-q4.gguf"
MODEL_DIR = "models"
MODEL_PATH = os.path.join(MODEL_DIR, MODEL_FILENAME)
VECTOR_DB_DIR = "vectordb_phi3"

# PDF kontrolü
POSSIBLE_PDF_PATHS = ["nutuk.pdf", "pdfs/nutuk.pdf"]
PDF_PATH = None
for path in POSSIBLE_PDF_PATHS:
    if os.path.exists(path):
        PDF_PATH = path
        break

if not PDF_PATH:
    logger.error("❌ 'nutuk.pdf' bulunamadı. Lütfen dosyayı bu dizine veya 'pdfs/' klasörüne ekleyin.")
    sys.exit(1)

def download_model():
    """Phi-3 Mini GGUF modeli mevcut değilse indirir."""
    if not os.path.exists(MODEL_PATH):
        logger.info(f"⬇️ {MODEL_FILENAME} indiriliyor...")
        os.makedirs(MODEL_DIR, exist_ok=True)
        hf_hub_download(repo_id=MODEL_REPO, filename=MODEL_FILENAME, local_dir=MODEL_DIR)
        logger.info("✅ Model indirildi.")
    else:
        logger.info("✅ Model zaten mevcut.")

def setup_rag():
    """PDF'i işler, embedding'leri oluşturur ve RAG zincirini kurar."""
    
    # 1. PDF Yükle
    logger.info(f"📄 PDF yükleniyor: {PDF_PATH}")
    loader = PyPDFLoader(PDF_PATH)
    documents = loader.load()
    logger.info(f"   {len(documents)} sayfa yüklendi.")

    # 2. Metni Parçala
    logger.info("✂️ Metin parçalanıyor...")
    text_splitter = RecursiveCharacterTextSplitter(chunk_size=1000, chunk_overlap=200)
    texts = text_splitter.split_documents(documents)
    logger.info(f"   {len(texts)} parça oluşturuldu.")

    # 3. Embedding'leri Oluştur
    logger.info("🧠 Embedding modeli yükleniyor (all-MiniLM-L6-v2)...")
    embeddings = HuggingFaceEmbeddings(model_name="all-MiniLM-L6-v2")
    
    # 4. Vektör Veritabanı
    logger.info("💾 Vektör veritabanı oluşturuluyor (ChromaDB)...")
    # Veritabanı zaten varsa yükle? Basitlik adına yeniden oluşturuyoruz veya yüklüyoruz.
    # Chroma, persist_directory ayarlandıysa kalıcılığı yönetir.
    db = Chroma.from_documents(texts, embeddings, persist_directory=VECTOR_DB_DIR)
    
    # 5. LLM Yükle
    logger.info("🤖 Phi-3 Mini modeli yükleniyor...")
    llm = LlamaCpp(
        model_path=MODEL_PATH,
        temperature=0.1,
        max_tokens=512,
        n_ctx=4096, # Phi-3 bağlam penceresi
        verbose=False
    )

    # 6. QA Zincirini Kur
    # Phi-3 Talimat İstemi Şablonu
    template = """<|user|>
Aşağıdaki bağlamı kullanarak soruyu cevapla. Cevabı bilmiyorsan bilmiyorum de.

Bağlam:
{context}

Soru:
{question}
<|end|>
<|assistant|>
"""
    prompt = PromptTemplate(template=template, input_variables=["context", "question"])

    qa = RetrievalQA.from_chain_type(
        llm=llm,
        chain_type="stuff",
        retriever=db.as_retriever(search_kwargs={"k": 3}),
        return_source_documents=True,
        chain_type_kwargs={"prompt": prompt}
    )
    
    return qa

def main():
    print("🚀 Nutuk RAG Sistemi Başlatılıyor...")
    download_model()
    
    qa_chain = setup_rag()
    
    print("\n✅ Sistem hazır! Nutuk hakkında sorular sorabilirsiniz. (Çıkış için 'q' veya 'exit')")
    print("-" * 50)
    
    while True:
        try:
            query = input("\nSoru: ").strip()
            if not query:
                continue
            if query.lower() in ['q', 'exit', 'quit']:
                print("👋 Görüşmek üzere!")
                break
            
            print("⏳ Düşünüyor...")
            res = qa_chain.invoke({"query": query})
            answer = res['result']
            
            print(f"\n🤖 Cevap: {answer}")
            
            # Kaynakları görmek için yorumu kaldırın
            # print("\nKaynaklar:")
            # for doc in res['source_documents']:
            #     print(f"- Sayfa {doc.metadata.get('page', '?')}: {doc.page_content[:100]}...")
                
        except KeyboardInterrupt:
            print("\n👋 İşlem iptal edildi.")
            break
        except Exception as e:
            logger.error(f"Bir hata oluştu: {e}")

if __name__ == "__main__":
    main()
