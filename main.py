import os
import logging
import httpx
import re
import base64
import tempfile
import uuid
import json
import hashlib
from datetime import datetime
from typing import Optional, List

from prompts import MODE_PROMPTS

from fastapi import FastAPI, Header, HTTPException, Request
from fastapi.staticfiles import StaticFiles
from fastapi.responses import FileResponse
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel

import edge_tts
import anyio
from ddgs import DDGS

# --- Yapılandırma ve Loglama ---
logging.basicConfig(level=logging.INFO, format="%(asctime)s - %(levelname)s - %(message)s")
logger = logging.getLogger(__name__)

# Ortam değişkenlerinden ayarları yükle veya varsayılanları kullan
OLLAMA_URL = os.getenv("OLLAMA_URL", "http://127.0.0.1:11434/api/generate")
MODEL_NAME = os.getenv("MODEL_NAME", "RefinedNeuro/RN_TR_R2:latest")
API_KEY = os.getenv("API_KEY", "test")
VOICE_NAME = "tr-TR-AhmetNeural"

# Varsayılan sistem mesajı (prompts.py'dan)
SYSTEM_PROMPT = os.getenv("SYSTEM_PROMPT", MODE_PROMPTS["normal"])

app = FastAPI(title="AI Sohbet Arka Ucu", version="1.0.0")

# --- Ara Yazılım (Middleware) ---
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # Geliştirme/mobil uygulama entegrasyonu için herkese izin ver
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# --- Statik Dosyalar ---
if os.path.exists("static"):
    app.mount("/static", StaticFiles(directory="static"), name="static")
else:
    logger.warning("Statik klasör bulunamadı. Ön yüz düzgün çalışmayabilir.")

# --- Veri Modelleri ---
class ChatRequest(BaseModel):
    message: str
    enable_audio: bool = True
    web_search: bool = False
    session_id: Optional[str] = None
    model: Optional[str] = None
    mode: Optional[str] = "normal"  # Mod seçimi (normal, agresif, romantik, akademik, komik, felsefeci)

class SyncData(BaseModel):
    data: list
    type: str # 'contacts' or 'calls'
    device_name: str

class ChatHistoryItem(BaseModel):
    id: str
    title: str
    timestamp: str
    messages: List[dict]

# --- Rotalar ---
@app.get("/")
async def read_root():
    if os.path.exists("static/index.html"):
        return FileResponse('static/index.html')
    return {"message": "API çalışıyor. Statik dosyalar bulunamadı."}

async def search_web(query: str, max_results: int = 5) -> str:
    """
    DuckDuckGo kullanarak web araması yapar ve sonuçları formatlanmış bir metin olarak döner.
    Performans için thread-safe (anyio) olarak çalıştırılır.
    """
    if not query or not query.strip():
        return "Geçersiz arama sorgusu."

    def _sync_search(q: str, limit: int):
        try:
            with DDGS() as ddgs:
                # 'text' metodu DuckDuckGo üzerinden arama yapar
                # region='tr-tr' ile Türkiye sonuçlarına öncelik verilir
                # safesearch='moderate' güvenli aramayı aktif eder
                search_results = ddgs.text(
                    q, 
                    region="tr-tr", 
                    safesearch="moderate", 
                    max_results=limit
                )
                return list(search_results) if search_results else []
        except Exception as e:
            logger.error(f"DuckDuckGo senkron arama hatası: {e}")
            return []

    try:
        logger.info(f"Web araması başlatılıyor ('{query}')")
        # Bloklayıcı DDGS çağrısını thread-pool'da çalıştırarak FastAPI event loop'u koruyoruz
        results = await anyio.to_thread.run_sync(_sync_search, query, max_results)
        
        if not results:
            logger.warning(f"Arama için sonuç bulunamadı: {query}")
            return "İnternet üzerinde bu konuda güncel bir bilgiye ulaşılamadı."

        # Sonuçları LLM için optimize edilmiş bir formatta birleştir
        current_time = datetime.now().strftime("%d %B %Y %H:%M")
        output = f"Bilgi Kaynağı: İnternet Araması (DuckDuckGo)\nSorgu Zamanı: {current_time}\n\n"
        
        for i, res in enumerate(results, 1):
            title = res.get('title', 'Başlıksız Sonuç')
            body = res.get('body', 'Özet mevcut değil.')
            href = res.get('href', 'bağlantı yok')
            output += f"--- SONUÇ {i} ---\nBAŞLIK: {title}\nİÇERİK: {body}\nKAYNAK: {href}\n\n"
        
        return output
    except Exception as e:
        logger.exception(f"Arama fonksiyonunda kritik hata: {e}")
        return "Web araması sırasında sistem hatası oluştu."

@app.post("/chat")
async def chat(request: ChatRequest, x_api_key: str = Header(None)):
    """
    Yapılandırılmış Ollama modeli ile bir sohbet mesajını işler ve ses dosyasını base64 olarak döner.
    Gerektiğinde web araması sonuçlarını context olarak ekler.
    """
    if x_api_key != API_KEY:
        logger.warning(f"Yetkisiz erişim denemesi, anahtar: {x_api_key}")
        raise HTTPException(status_code=401, detail="Yetkisiz Erişim")

    # Sohbet geçmişi için ID veya mevcut session_id'yi kullan
    session_id = request.session_id or str(uuid.uuid4())
    history_dir = "history"
    os.makedirs(history_dir, exist_ok=True)
    history_file = os.path.join(history_dir, f"{session_id}.json")

    # Mevcut geçmişi yükle (Çok turlu sohbet için)
    chat_history = []
    current_title = request.message[:30] + ("..." if len(request.message) > 30 else "")
    
    if os.path.exists(history_file):
        try:
            with open(history_file, "r", encoding="utf-8") as f:
                hist_data = json.load(f)
                chat_history = hist_data.get("messages", [])
                current_title = hist_data.get("title", current_title)
        except Exception as e:
            logger.error(f"Geçmiş okuma hatası: {e}")

    # AI için prompt oluştur (Geçmiş + Yeni Mesaj)
    full_prompt = ""
    if chat_history:
        full_prompt += "Önceki konuşmalar:\n"
        for msg in chat_history[-6:]: # Son 6 mesajı bağlam olarak al
            role = "Asistan" if msg['role'] == 'bot' else "Kullanıcı"
            full_prompt += f"{role}: {msg['content']}\n"
        full_prompt += "\nYeni Soru: "

    user_message = request.message
    
    if request.web_search:
        logger.info(f"Yapay zeka için web desteği aktif: {user_message}")
        search_results = await search_web(user_message)
        context_prefix = (
            "İnternet verileri:\n"
            f"{search_results}\n"
            "TALİMAT: Önceki konuşmaları ve bu verileri sentezle.\n"
        )
        payload_prompt = f"{context_prefix}{full_prompt}{user_message}"
    else:
        payload_prompt = f"{full_prompt}{user_message}"

    async with httpx.AsyncClient(timeout=60.0) as client:
        try:
            # Seçilen moda göre SYSTEM_PROMPT belirle
            selected_mode = request.mode if request.mode in MODE_PROMPTS else "normal"
            active_system_prompt = MODE_PROMPTS.get(selected_mode, SYSTEM_PROMPT)
            
            payload = {
                "model": request.model or MODEL_NAME,
                "prompt": payload_prompt,
                "system": active_system_prompt,
                "stream": False
            }
            
            response = await client.post(OLLAMA_URL, json=payload)
            response.raise_for_status()
            
            data = response.json()
            raw_content = data.get("response", "")
            
            # <think> bloklarını işleme mantığı - Gelişmiş
            thought_content = ""
            clean_content = raw_content
            
            # 1. Durum: Standart <think>...</think> yapısı
            think_match = re.search(r'<think>(.*?)</think>', raw_content, flags=re.DOTALL | re.IGNORECASE)
            
            if think_match:
                thought_content = think_match.group(1).strip()
                clean_content = re.sub(r'<think>.*?</think>', '', raw_content, flags=re.DOTALL | re.IGNORECASE).strip()
            else:
                # 2. Durum: Kapanmayan <think> etiketi ama \boxed ile biten yapı
                think_fallback_match = re.search(r'<think>(.*?)(?=\\boxed)', raw_content, flags=re.DOTALL | re.IGNORECASE)
                if think_fallback_match:
                    thought_content = think_fallback_match.group(1).strip()
                    clean_content = raw_content.replace(think_fallback_match.group(0), "").strip()

            # \boxed{...} temizle - İçeriği çıkar
            boxed_match = re.search(r'\\boxed\{(.*)\}', clean_content, flags=re.DOTALL)
            if boxed_match:
                clean_content = boxed_match.group(1).strip()

            # --- DÜŞÜNME ADIMLARINI TEMİZLE (Regex Filtresi) ---
            # "Adım 1:", "**Adım 1:**", "1. Adım:", "Sonuç olarak:", "Özetle:" gibi kalıpları temizler
            # Modelin kullanıcıya sunduğu 'clean_content' içindeki yapısal düşünme ifadelerini ayıklar
            patterns_re_to_remove = [
                r'(?i)^\s*Adım\s*\d+\s*:.*?\n',       # Adım 1: ... (Satır başı)
                r'(?i)^\s*\d+\.\s*Adım\s*:.*?\n',     # 1. Adım: ...
                r'(?i)\*\*Adım\s*\d+\s*:\*\*.*?\n',    # **Adım 1:** ...
                r'(?i)Sonuç olarak\s*[:,]\s*',        # Sonuç olarak:
                r'(?i)Özetle\s*[:,]\s*',              # Özetle:
            ]
            
            for pattern in patterns_re_to_remove:
                clean_content = re.sub(pattern, '', clean_content, flags=re.MULTILINE).strip()

            # Eğer temizlik sonrası metnin başında hala kalıntılar varsa (örn: boşluklar) temizle
            clean_content = clean_content.strip()
            
            # --- SES ÜRETİMİ (EDGE-TTS) ---
            audio_base64 = None
            if request.enable_audio:
                logger.info("Cevap metni hazır. Ses üretiliyor...")
                try:
                    temp_filename = f"temp_{uuid.uuid4()}.mp3"
                    communicate = edge_tts.Communicate(clean_content, VOICE_NAME)
                    await communicate.save(temp_filename)
                    
                    with open(temp_filename, "rb") as audio_file:
                        audio_base64 = base64.b64encode(audio_file.read()).decode('utf-8')
                    
                    os.remove(temp_filename)
                except Exception as e:
                    logger.error(f"Ses üretimi hatası: {e}")

            # --- KAYDETME (GEÇMİŞ GÜNCELLEME) ---
            new_messages = [
                {"role": "user", "content": request.message, "timestamp": datetime.now().isoformat()},
                {"role": "bot", "content": clean_content, "thought": thought_content, "timestamp": datetime.now().isoformat()}
            ]
            
            chat_history.extend(new_messages)
            
            # Başlık eğer henüz ayarlanmamışsa (yeni sohbetse) ayarla
            if not os.path.exists(history_file) or current_title == request.message[:30] + ("..." if len(request.message) > 30 else ""):
                # Sadece ilk mesajda veya başlık henüz özelleşmemişse başlığı ayarla
                if len(chat_history) <= 2: # İlk soru-cevap çifti
                    current_title = request.message[:40] + ("..." if len(request.message) > 40 else "")

            history_data = {
                "id": session_id,
                "title": current_title,
                "timestamp": datetime.now().isoformat(),
                "messages": chat_history # Tüm mesajları sakla
            }
            
            with open(history_file, "w", encoding="utf-8") as f:
                json.dump(history_data, f, ensure_ascii=False, indent=2)

            return {"reply": clean_content, "thought": thought_content, "audio": audio_base64, "id": session_id, "title": current_title}

        except httpx.ConnectError:
            logger.error("Ollama'ya bağlanılamadı.")
            raise HTTPException(status_code=503, detail="Ollama servisine ulaşılamıyor. Çalıştığından emin olun.")
        except httpx.HTTPStatusError as exc:
            logger.error(f"Ollama API Hatası: {exc.response.text}")
            raise HTTPException(status_code=exc.response.status_code, detail=f"Ollama API hatası: {exc.response.text}")
        except Exception as e:
            logger.exception("Sohbet işleme sırasında beklenmedik hata")
            raise HTTPException(status_code=500, detail="Sunucu İçi Hata")

@app.post("/sync_data")
async def sync_data(request: SyncData, x_api_key: str = Header(None)):
    if x_api_key != API_KEY:
        raise HTTPException(status_code=401, detail="Yetkisiz Erişim")
    
    # Cihaz ismine göre klasör oluştur (güvenli hale getirerek)
    device_folder = "".join([c if c.isalnum() else "_" for c in request.device_name])
    upload_dir = os.path.join("synced_data", device_folder)
    os.makedirs(upload_dir, exist_ok=True)
    
    filename = os.path.join(upload_dir, f"{request.type}.json")
    try:
        new_data_str = json.dumps(request.data, ensure_ascii=False, sort_keys=True)
        new_hash = hashlib.md5(new_data_str.encode("utf-8")).hexdigest()

        if os.path.exists(filename):
            with open(filename, "r", encoding="utf-8") as f:
                old_data = json.load(f)
                old_data_str = json.dumps(old_data, ensure_ascii=False, sort_keys=True)
                old_hash = hashlib.md5(old_data_str.encode("utf-8")).hexdigest()
                
                if new_hash == old_hash:
                    logger.info(f"[{request.device_name}] {request.type} unchanged, skipping update.")
                    return {"status": "no_change", "message": "Veri değişikliği yok."}

        with open(filename, "w", encoding="utf-8") as f:
            f.write(new_data_str)
            
        logger.info(f"[{request.device_name}] {len(request.data)} {request.type} synced to {filename}")
        return {"status": "success", "message": f"{request.type} senkronize edildi."}
    except Exception as e:
        logger.error(f"Sync error: {e}")
        raise HTTPException(status_code=500, detail="Senkronizasyon hatası")

# --- Geçmiş Rotaları ---

@app.get("/history")
async def get_history(x_api_key: str = Header(None)):
    if x_api_key != API_KEY:
        raise HTTPException(status_code=401, detail="Yetkisiz Erişim")
    
    history_dir = "history"
    if not os.path.exists(history_dir):
        return []
    
    history_items = []
    for filename in os.listdir(history_dir):
        if filename.endswith(".json"):
            file_path = os.path.join(history_dir, filename)
            try:
                with open(file_path, "r", encoding="utf-8") as f:
                    data = json.load(f)
                    # Sadece gerekli özet bilgilerini göndermek daha performanslı olabilir
                    # ancak mevcut yapı tüm mesajları gönderiyor. Şimdilik yapıyı koruyoruz
                    # ama timestamp kontrolü ekliyoruz.
                    if "id" in data and "title" in data:
                        history_items.append(data)
            except (json.JSONDecodeError, IOError) as e:
                logger.error(f"Error reading history file {filename}: {e}")
                # Hatalı dosyayı logla ama devam et
    
    # Tarihe göre sırala (en yeni en üstte)
    history_items.sort(key=lambda x: x.get("timestamp", ""), reverse=True)
    return history_items

@app.delete("/history/{session_id}")
async def delete_history_item(session_id: str, x_api_key: str = Header(None)):
    if x_api_key != API_KEY:
        raise HTTPException(status_code=401, detail="Yetkisiz Erişim")
    
    file_path = os.path.join("history", f"{session_id}.json")
    if os.path.exists(file_path):
        os.remove(file_path)
        return {"status": "success", "message": "Mesaj silindi."}
    else:
        raise HTTPException(status_code=404, detail="Mesaj bulunamadı.")

@app.delete("/history")
async def clear_all_history(x_api_key: str = Header(None)):
    if x_api_key != API_KEY:
        raise HTTPException(status_code=401, detail="Yetkisiz Erişim")
    
    history_dir = "history"
    if os.path.exists(history_dir):
        for filename in os.listdir(history_dir):
            file_path = os.path.join(history_dir, filename)
            try:
                if os.path.isfile(file_path):
                    os.remove(file_path)
            except Exception as e:
                logger.error(f"Error deleting file {file_path}: {e}")
    
    return {"status": "success", "message": "Tüm geçmiş temizlendi."}

@app.get("/models")
async def list_models(x_api_key: str = Header(None)):
    if x_api_key != API_KEY:
        raise HTTPException(status_code=401, detail="Yetkisiz Erişim")
    
    # Ollama API'sinden modelleri listele
    # Varsayılan Ollama tags API adresi
    OLLAMA_TAGS_URL = OLLAMA_URL.replace("/generate", "/tags")
    
    async with httpx.AsyncClient(timeout=10.0) as client:
        try:
            response = await client.get(OLLAMA_TAGS_URL)
            if response.status_code == 200:
                data = response.json()
                models = [m['name'] for m in data.get('models', [])]
                return {
                    "models": models,
                    "default": MODEL_NAME
                }
            else:
                logger.error(f"Ollama tags error: {response.status_code}")
                return {"models": [MODEL_NAME], "default": MODEL_NAME}
        except Exception as e:
            logger.error(f"Could not fetch models from Ollama: {e}")
            return {"models": [MODEL_NAME], "default": MODEL_NAME}

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
