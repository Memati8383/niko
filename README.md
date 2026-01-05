# 🤖 Niko AI Asistant

**Niko**, FastAPI tabanlı güçlü bir arka uç ve modern bir web arayüzü ile çalışan, Ollama destekli, yerel ve gizlilik odaklı bir yapay zeka asistanıdır.

---

## 🚀 Öne Çıkan Özellikler

- **🎭 Çoklu Kişilik (Modlar):** 8 farklı karakter modu (Agresif, Bilge, Dahi, Romantik vb.) ile farklı kullanım senaryoları.
- **🧠 Düşünce Süreci Görüntüleme:** Modelin akıl yürütme adımlarını (RefinedNeuro/RN_TR_R2:latest vb.) kullanıcı arayüzünde şeffaf bir şekilde görebilme.
- **🌐 Gerçek Zamanlı Web Araması:** Güncel bilgilere erişmek için DuckDuckGo entegrasyonu ile internette arama yapabilme.
- **📚 RAG (Bilgi Erişim Desteği):** Yerel belgelerden (PDF, TXT vb.) bilgi sorgulama ve bağlama dayalı yanıt üretme.
- **💾 Gelişmiş Sohbet Geçmişi:** Sohbetleri yerel olarak JSON formatında saklama, geri yükleme ve yönetme (CRUD desteği).
- **📥 Sohbet Dışa Aktarma:** Sohbetleri Markdown (.md) formatında döküman olarak kaydedebilme.
- **🎙️ Sesli Yanıt (TTS):** Microsoft Edge TTS teknolojisi ile doğal ve akıcı Türkçe ses sentezleme.
- **💎 Premium UI/UX:** Glassmorphism tasarımı, karanlık mod, responsive yapı ve gelişmiş Markdown render.
- **💻 Kod Analizi:** Syntax highlighting (highlight.js) ile kod bloklarını şık ve okunabilir formatta görüntüleme.
- **🧪 Forensics & Test Suite:** AI performansını ve agresiflik seviyelerini ölçen gelişmiş test araçları.

---

## 📂 Proje Yapısı

```text
├── main.py                          # FastAPI Arka Uç (API & Mantık)
├── prompts.py                       # AI Karakter Modları ve Sistem Mesajları
├── start_tunnel.py                  # Cloudflare Tunnel otomasyon scripti
├── history/                         # Sohbet geçmişlerinin saklandığı klasör (JSON)
├── static/                          # Web Ön Yüz Dosyaları
│   ├── index.html                   # Ana Arayüz
│   ├── style.css                    # Gelişmiş CSS (Glassmorphism & Animasyonlar)
│   └── script.js                    # Dinamik Ön Yüz Mantığı
├── test.py                          # Temel API fonksiyonellik testi
├── yapay_zeka_agresiflik_testi.py    # Gelişmiş Agresyon & Performans Analizi
├── dashboard.html                   # Test sonuçlarını görselleştiren rapor ekranı
├── clean_pycache.py                 # Gereksiz önbellek dosyalarını temizleme aracı
└── requirements.txt                 # Bağımlılıklar
```

---

## 🎭 Niko'nun Modları (Personalities)

Niko, ruh halinize veya ihtiyacınıza göre farklı kimliklere bürünebilir:

| Mod           | Karakter Özelliği               | Kullanım Amacı                                |
| :------------ | :------------------------------ | :-------------------------------------------- |
| **Normal**    | Yardımsever & Profesyonel       | Günlük asistanlık görevleri.                  |
| **Agresif**   | Sözel Cellat (Hakaret İçerikli) | Eğlence veya stres atma (Dikkatli Kullanın).  |
| **Bilge**     | Sakin & Felsefeci               | Hayat üzerine derin sohbetler ve tavsiyeler.  |
| **Dahi**      | Analitik & Teknik               | Karmaşık matematiksel ve bilimsel problemler. |
| **Kibar**     | İstanbul Beyefendisi            | Son derece nazik ve saygılı hitabet.          |
| **Esprili**   | İronik & Şakacı                 | Stand-up tadında komik yanıtlar.              |
| **Kodlayıcı** | Yazılım Mühendisi               | Bug ayıklama ve algoritma geliştirme.         |
| **Romantik**  | Şair Ruhlu & Duygusal           | Şiirsel ve sevgi dolu yaklaşımlar.            |

---

## 🔌 API Dokümantasyonu

API güvenliği için tüm isteklerde `x-api-key: test` (varsayılan) header'ı gönderilmelidir.

### Ana Endpoint'ler

| Endpoint               | Metod    | Açıklama                                   |
| :--------------------- | :------- | :----------------------------------------- |
| `/chat`                | `POST`   | AI ile sohbet et (Web/RAG desteği ile).    |
| `/history`             | `GET`    | Tüm kayıtlı sohbet geçmişini listele.      |
| `/history/{id}`        | `DELETE` | Belirli bir sohbet geçmişini sil.          |
| `/history`             | `DELETE` | Tüm geçmişi temizle.                       |
| `/export/{session_id}` | `GET`    | Sohbeti Markdown (.md) olarak indir.       |
| `/models`              | `GET`    | Ollama üzerindeki yüklü modelleri listele. |

### Sohbet İsteği Parametreleri:

```json
{
  "message": "Naber Niko?",
  "mode": "bilge", // normal, agresif, bilge, dahi, kibar, esprili, kodlayici, romantik
  "web_search": true, // İnternet araması aktif (DuckDuckGo)
  "rag_search": true, // Yerel belge veritabanı araması aktif
  "enable_audio": true, // Sesli yanıt üretimi (Edge-TTS)
  "model": "RefinedNeuro/RN_TR_R2:latest", // Opsiyonel: Model seçimi
  "session_id": "uuid" // Mevcut sohbetin devamı için
}
```

---

## 📚 RAG ve Dosya Analizi

Niko, `ChromaDB` kullanarak yerel bir bilgi tabanı oluşturabilir. `rag/` dizini altında saklanan vektör veritabanı sayesinde, model eğitim verisinde olmayan güncel veya özel bilgilere erişebilir. Özellikle tıbbi, teknik veya kişisel dökümanların analizinde yüksek başarı sağlar.

---

## 🧪 Forensics & Ölçümleme

`yapay_zeka_agresiflik_testi.py` aracı ile modelin yanıt kalitesi, agresyon düzeyi ve karakter tutarlılığı analiz edilebilir.

- **Otomatik Test:** 10 farklı kategoride model performansını ölçer.
- **Raporlama:** Test sonuçları `dashboard.html` üzerinden grafiksel olarak izlenebilir.

---

## 🛠️ Kurulum ve Başlatma

### 1. Kurulum

```bash
pip install -r requirements.txt
```

### 2. Çalıştırma

```bash
# Servisi başlatın
python main.py
```

Arayüze erişin: `http://localhost:8000`

---

## 🗺️ Yol Haritası

- [x] **İnternet Araması:** DuckDuckGo entegrasyonu.
- [x] **Çoklu Karakter:** 8 farklı AI modu eklendi.
- [x] **Düşünce Süreci:** Akıl yürütme blokları görselleştirildi.
- [x] **Sohbet Dışa Aktarma:** Markdown formatında indirme desteği.
- [x] **Sesli Yanıt:** Microsoft Edge TTS entegrasyonu.
- [x] **Forensics Suite:** Agresyon testi ve interaktif raporlama.
- [x] **Cloudflare Tunnel:** start_tunnel.py ile güvenli uzaktan erişim.
- [x] **Sohbet Yönetimi:** Tam kapsamlı geçmiş yönetimi ve arşivleme.
- [x] **RAG Sistemi:** ChromaDB ile yerel belge analizi (Kısmen yayında).
- [ ] **Görüntü İşleme:** Vision modelleri ile görsel analiz desteği.
- [ ] **Sesli Komut:** Mikrofon üzerinden doğrudan sesli komut alımı.

---

## ⚙️ Yapılandırma

`main.py` veya ENV üzerinden özelleştirilebilir. Proje kök dizininde bir `.env` dosyası oluşturarak aşağıdaki ayarları tanımlayabilirsiniz:

```env
# Kullanılacak LLM Modeli
MODEL_NAME=RefinedNeuro/RN_TR_R2:latest

# Sunucu Güvenlik Anahtarı
API_KEY=test

# Ollama API Adresi
OLLAMA_URL=http://127.0.0.1:11434/api/generate

# Varsayılan Sistem Mesajı
SYSTEM_PROMPT="Senin adın Niko. Sen yardımsever, zeki ve profesyonel bir yapay zeka asistanısın."
```

---

> [!TIP] > **Cloudflare Kullanımı:** `python start_tunnel.py` komutu ile yerel sunucunuzu internete açabilir ve güncel linke her zaman bu README üzerinden erişebilirsiniz.

> 🌐 **Güncel Tünel Adresi:** [https://ron-nickname-wine-emotions.trycloudflare.com](https://ron-nickname-wine-emotions.trycloudflare.com)
