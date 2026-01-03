# Niko AI Asistan

**Niko**, FastAPI tabanlı güçlü bir arka uç ve modern bir web arayüzü ile çalışan, Ollama destekli kişisel bir yapay zeka asistanıdır.

## 🚀 Özellikler

- **Zeki Sohbet Botu:** Ollama ile yerel LLM (RefinedNeuro/RN_TR_R2:latest vb.) entegrasyonu sayesinde gizlilik odaklı ve hızlı yanıtlar.
- **Sesli Yanıt:** `edge-tts` kullanarak yüksek kaliteli, gerçekçi Türkçe ses sentezleme (TTS).
- **Modern Web Arayüzü:** Karanlık mod destekli, Markdown çıktılarını şık bir şekilde render eden, cam efektli (glassmorphism) responsive tasarım.
- **Hızlı API:** FastAPI mimarisi ile asenkron ve düşük gecikmeli veri akışı.
- **Dinamik İçerik:** Kod blokları için sözdizimi vurgulama (syntax highlighting) ve matematiksel formüller için destek.
- **Mobil Uyumluluk:** Hem web hem de Android uygulaması üzerinden kesintisiz erişim.
- **Genişletilebilir Yapı:** Kolayca yeni araçlar (internet araması, dosya analizi vb.) eklenebilir modüler mimari.

## 📂 Proje Yapısı

- `main.py`: Projenin ana FastAPI arka uç dosyası.
- `static/`: Web arayüzü için gerekli HTML, CSS ve JavaScript dosyaları.

## 🛠️ Kurulum ve Çalıştırma

### Gereksinimler

- Python 3.8+
- [Ollama](https://ollama.ai/) (Yerel makinede çalışıyor olmalı)
- Gerekli Python kütüphanelerini otomatik yükleyin:

  ```bash
  pip install -r requirements.txt
  ```

- Gerekli Python kütüphanelerini yükleyin:
  ```bash
  pip install fastapi uvicorn httpx edge-tts pydantic
  ```

### Arka Ucu Çalıştırma

1. Ollama sunucusunu başlatın:

   ```bash
   ollama serve
   ```

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

Sunucunuza dışarıdan erişebilmek ve **bu README dosyasındaki linki otomatik güncellemek** için aşağıdaki scripti çalıştırın:

```bash
python start_tunnel.py
```

Alternatif olarak manuel komut:

```bash
cloudflared tunnel --url http://127.0.0.1:8000
```

Bu komut size `https://....trycloudflare.com` uzantılı rastgele bir URL verecektir.

> [!IMPORTANT] > **Ağ Geçidi Aktif**
> 🌐 **Güncel Tünel Adresi:** [https://ron-nickname-wine-emotions.trycloudflare.com](https://ron-nickname-wine-emotions.trycloudflare.com)

## 🗺️ Yol Haritası (Gelecek Özellikler)

Projenin gelişim sürecinde eklenmesi planlanan özellikler:

- [ ] **Gelişmiş Bellek:** Kullanıcıyla olan geçmiş konuşmaları daha iyi hatırlayan uzun süreli hafıza.
- [ ] **İnternet Araması:** Gerçek zamanlı bilgi erişimi için Google/DuckDuckGo entegrasyonu.
- [ ] **Görüntü İşleme:** Gönderilen görselleri analiz etme ve betimleme yeteneği.
- [ ] **Dosya Analizi:** PDF, TXT ve CSV dosyalarını okuyup özetleme desteği.
- [ ] **Plugin Sistemi:** Üçüncü parti servisler (Spotify, Google Takvim vb.) için eklenti desteği.
- [ ] **Daha Fazla Yerel Model:** Farklı donanımlar için optimize edilmiş model seçenekleri.

## ⚙️ Yapılandırma

`main.py` içindeki aşağıdaki ortam değişkenleri düzenlenebilir:

- `OLLAMA_URL`: Ollama API adresi (Varsayılan: `http://127.0.0.1:11434/api/generate`)
- `MODEL_NAME`: Kullanılan LLM modeli (Varsayılan: `RefinedNeuro/RN_TR_R2:latest`)
- `API_KEY`: Basit API anahtarı koruması (Varsayılan: `test`)
- `SYSTEM_PROMPT`: AI'ın kişiliğini belirleyen sistem mesajı.
