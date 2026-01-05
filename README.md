# 🤖 Niko AI Asistan

**Niko**, FastAPI tabanlı güçlü bir arka uç ve modern bir web arayüzü ile çalışan, Ollama destekli, yerel ve gizlilik odaklı bir yapay zeka asistanıdır.

---

## 🚀 Öne Çıkan Özellikler

- **🧠 Düşünce Süreci Görüntüleme:** Modelin akıl yürütme adımlarını (DeepSeek vb.) kullanıcı arayüzünde şeffaf bir şekilde görebilme.
- **🌐 Gerçek Zamanlı Web Araması:** Güncel bilgilere erişmek için DuckDuckGo entegrasyonu ile internette arama yapabilme.
- **💾 Gelişmiş Sohbet Geçmişi:** Sohbetleri yerel olarak JSON formatında saklama, geri yükleme ve yönetme (CRUD desteği).
- **🎙️ Sesli Yanıt (TTS):** Microsoft Edge TTS teknolojisi ile doğal ve akıcı Türkçe ses sentezleme.
- ** Premium UI/UX:** Glassmorphism (cam efekti) tasarımı, karanlık mod desteği, responsive yapı ve gelişmiş Markdown render.
- **💻 Kod Analizi:** Syntax highlighting (highlight.js) ile kod bloklarını şık ve okunabilir formatta görüntüleme.

---

## 📂 Proje Yapısı

```text
├── main.py              # FastAPI Arka Uç (API & Mantık)
├── start_tunnel.py      # Cloudflare Tunnel otomasyon scripti
├── history/             # Sohbet geçmişlerinin saklandığı klasör (JSON)
├── static/              # Web Ön Yüz Dosyaları
│   ├── index.html       # Ana Arayüz
│   ├── style.css        # Gelişmiş CSS (Glassmorphism & Animasyonlar)
│   └── script.js        # Dinamik Ön Yüz Mantığı
└── requirements.txt     # Bağımlılıklar
```

---

## 🛠️ Kurulum ve Başlatma

### 1. Sistem Gereksinimleri

- Python 3.8+
- [Ollama](https://ollama.ai/) (Yerel LLM sunucusu)
- **Tavsiye Edilen Model:** `RefinedNeuro/RN_TR_R2:latest` veya `deepseek-v3`

### 2. Kurulum

Gerekli paketleri çalışma dizininde yükleyin:

```bash
pip install -r requirements.txt
```

### 3. Çalıştırma

Önce Ollama'yı, ardından servisi başlatın:

```bash
# Ollama'yı başlatın
ollama serve

# Niko'yu başlatın
python main.py
```

Arayüze erişin: `http://localhost:8000`

---

## 🔌 API Dokümantasyonu

API güvenliği için tüm isteklerde `x-api-key: test` (varsayılan) header'ı gönderilmelidir.

| Endpoint        | Metod    | Açıklama                                   |
| :-------------- | :------- | :----------------------------------------- |
| `/chat`         | `POST`   | AI ile sohbet et. (Arama ve Ses opsiyonel) |
| `/history`      | `GET`    | Tüm kayıtlı sohbet geçmişini listele.      |
| `/history/{id}` | `DELETE` | Belirli bir sohbet geçmişini sil.          |
| `/history`      | `DELETE` | Tüm geçmişi temizle.                       |

### Örnek Sohbet İsteği:

```json
{
  "message": "Bugün hava nasıl?",
  "web_search": true,
  "enable_audio": false,
  "session_id": "opsiyonel-uuid"
}
```

---

## 🗺️ Yol Haritası

- [x] **İnternet Araması:** DuckDuckGo entegrasyonu tamamlandı.
- [x] **Sohbet Geçmişi:** Kalıcı oturum desteği eklendi.
- [x] **Düşünce Süreci:** Akıl yürütme blokları görselleştirildi.
- [ ] **Görüntü İşleme:** Vision modelleri ile görsel analiz desteği.
- [ ] **Dosya Analizi:** PDF, TXT ve CSV dosyalarını sorgulama yeteneği.
- [ ] **Plugin Sistemi:** Spotify ve Google Takvim entegrasyonu.
- [ ] **Sesli Komut:** Mikrofon üzerinden doğrudan konuşma desteği.

---

## ⚙️ Yapılandırma

`main.py` içerisindeki varsayılan ayarları ortam değişkenleri (ENV) ile değiştirebilirsiniz:

- `MODEL_NAME`: Kullanılacak LLM (Örn: `llama3`, `mistral`)
- `API_KEY`: Güvenlik anahtarı (Varsayılan: `test`)
- `VOICE_NAME`: TTS ses seçeneği (Örn: `tr-TR-EmelNeural`)

---

> [!TIP] > **Cloudflare Kullanımı:** `python start_tunnel.py` komutu ile yerel sunucunuzu hiçbir ağ ayarı yapmadan internete güvenle açabilir ve güncel linke her zaman bu README üzerinden erişebilirsiniz.

🌐 **Güncel Tünel Adresi:** [https://ron-nickname-wine-emotions.trycloudflare.com](https://ron-nickname-wine-emotions.trycloudflare.com)
