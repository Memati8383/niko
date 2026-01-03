import os
import logging
import httpx
import re
from fastapi import FastAPI, Header, HTTPException, Request
from fastapi.staticfiles import StaticFiles
from fastapi.responses import FileResponse
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from typing import Optional

import edge_tts
import base64
import tempfile
import uuid

# --- Yapılandırma ve Loglama ---
logging.basicConfig(level=logging.INFO, format="%(asctime)s - %(levelname)s - %(message)s")
logger = logging.getLogger(__name__)

# Ortam değişkenlerinden ayarları yükle veya varsayılanları kullan
OLLAMA_URL = os.getenv("OLLAMA_URL", "http://127.0.0.1:11434/api/generate")
MODEL_NAME = os.getenv("MODEL_NAME", "RefinedNeuro/RN_TR_R2:latest")
API_KEY = os.getenv("API_KEY", "test")
VOICE_NAME = "tr-TR-AhmetNeural"

# Sistem Mesajını Tanımla
SYSTEM_PROMPT = os.getenv("SYSTEM_PROMPT", (
    "Senin adın Niko. Sen yardımsever, zeki, esprili ve profesyonel bir yapay zeka asistanısın. "
    "Her zaman Türkçe cevap ver. Cevapların kullanıcı dostu, net ve bilgilendirici olsun. "
    "Karmaşık konuları basitçe açıkla. Kullanıcıyla samimi ama saygılı bir dil kullan."
))

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

class SyncData(BaseModel):
    data: list
    type: str # 'contacts' or 'calls'
    device_name: str

# --- Rotalar ---
@app.get("/")
async def read_root():
    if os.path.exists("static/index.html"):
        return FileResponse('static/index.html')
    return {"message": "API çalışıyor. Statik dosyalar bulunamadı."}

@app.post("/chat")
async def chat(request: ChatRequest, x_api_key: str = Header(None)):
    """
    Yapılandırılmış Ollama modeli ile bir sohbet mesajını işler ve ses dosyasını base64 olarak döner.
    """
    if x_api_key != API_KEY:
        logger.warning(f"Yetkisiz erişim denemesi, anahtar: {x_api_key}")
        raise HTTPException(status_code=401, detail="Yetkisiz Erişim")

    logger.info(f"Mesaj alındı: {request.message[:50]}...")

    async with httpx.AsyncClient(timeout=60.0) as client:
        try:
            payload = {
                "model": MODEL_NAME,
                "prompt": request.message,
                "system": SYSTEM_PROMPT,
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
                # <think> ile başlar, \boxed'e kadar olan kısmı alır
                think_fallback_match = re.search(r'<think>(.*?)(?=\\boxed)', raw_content, flags=re.DOTALL | re.IGNORECASE)
                if think_fallback_match:
                    thought_content = think_fallback_match.group(1).strip()
                    # <think> bloğunu temizle, geriye \boxed'li kısım kalsın
                    clean_content = raw_content.replace(think_fallback_match.group(0), "").strip()

            # \boxed{...} temizle - İçeriği çıkar
            # Greedy match (.*) kullanarak en son kapanan paranteze kadar alır
            boxed_match = re.search(r'\\boxed\{(.*)\}', clean_content, flags=re.DOTALL)
            if boxed_match:
                clean_content = boxed_match.group(1).strip()
            
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
                    logger.error(f"Ses üretim hatası: {e}")
                    # Hata olsa bile metni dön
            else:
                logger.info("Ses üretimi istemci tarafından devre dışı bırakıldı.")
            
            return {"reply": clean_content, "thought": thought_content, "audio": audio_base64}

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
    import json
    import hashlib

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

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
