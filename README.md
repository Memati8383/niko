# Niko AI Asistan

**Niko**, FastAPI tabanlı güçlü bir arka uç ve modern bir web arayüzü ile çalışan, Ollama destekli kişisel bir yapay zeka asistanıdır.

## 🚀 Özellikler

- **Zeki Sohbet Botu:** Ollama ile yerel LLM (DeepSeek-R1-Distill-Qwen-14B vb.) entegrasyonu.
- **Sesli Yanıt:** `edge-tts` kullanarak gerçekçi Türkçe ses sentezleme (TTS).
- **Web Arayüzü:** Markdown destekli, şık ve duyarlı (responsive) modern web arayüzü.
- **Acil Durum Bilgisi:** Web arayüzünde entegre triyaj ve acil durum bilgilendirme paneli.

## 📂 Proje Yapısı

- `main.py`: Projenin ana FastAPI arka uç dosyası.
- `static/`: Web arayüzü için gerekli HTML, CSS ve JavaScript dosyaları.
- `.github/`: (İsteğe bağlı) GitHub Actions veya şablon dosyaları.

## 🛠️ Kurulum ve Çalıştırma

### Gereksinimler

- Python 3.8+
- [Ollama](https://ollama.ai/) (Yerel makinede çalışıyor olmalı)
- Gerekli Python kütüphaneleri:

  ```bash
  pip install -r requirements.txt
  ```

- Gerekli Python kütüphaneleri:
  ```bash
  pip install fastapi uvicorn httpx edge-tts pydantic
  ```

### Arka Ucu Çalıştırma

1. Ollama sunucusunun çalıştığından emin olun (varsayılan: port 11434).
2. API'yi başlatın:

   ```bash
   python main.py
   ```

   _veya_

   ```bash
   uvicorn main:app --host 0.0.0.0 --port 8000 --reload
   ```

3. Web arayüzüne tarayıcınızdan erişin: [http://localhost:8000](http://localhost:8000)

### 🌐 Dışarıdan Erişim (Cloudflare Tunnel)

Sunucunuza dışarıdan erişebilmek için **Cloudflare Tunnel** kullanabilirsiniz:

```bash
cloudflared tunnel --url http://127.0.0.1:8000
```

Bu komut size `https://....trycloudflare.com` uzantılı rastgele bir URL verecektir.

**Güncel Tünel Adresi:** `https://streets-doom-atmospheric-relaxation.trycloudflare.com`

## ⚙️ Yapılandırma

`main.py` içindeki aşağıdaki ortam değişkenleri düzenlenebilir:

- `OLLAMA_URL`: Ollama API adresi (Varsayılan: `http://127.0.0.1:11434/api/generate`)
- `MODEL_NAME`: Kullanılan LLM modeli (Varsayılan: `RefinedNeuro/RN_TR_R2:latest`)
- `API_KEY`: Basit API anahtarı koruması (Varsayılan: `test`)
- `SYSTEM_PROMPT`: AI'ın kişiliğini belirleyen sistem mesajı.
