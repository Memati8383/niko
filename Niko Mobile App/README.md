# Niko Mobile App

Niko, Android cihazlar için geliştirilmiş, sesli komutlarla çalışan kişisel bir yapay zeka asistanıdır. Gelişmiş ses tanıma özellikleri ve yapay zeka entegrasyonu sayesinde telefonunuzu dokunmadan kontrol etmenizi sağlar.

## Özellikler

- **Sesli Sohbet:** Niko ile doğal dilde konuşabilir, sorular sorabilir ve AI tabanlı cevaplar alabilirsiniz.
- **Arama Yönetimi:**
  - Rehberdeki kişileri ismen arama ("Ahmet'i ara").
  - Son gelen veya son aranan numarayı geri arama.
- **WhatsApp Entegrasyonu:**
  - Gelen WhatsApp mesajlarını sesli okuma.
  - Mesajlara sesli komutla otomatik cevap verme.
- **Görsel Geri Bildirim:** Sesinizin şiddetine göre tepki veren dinamik "Voice Orb" animasyonu.
- **Sesli Yanıt:** Metin-Konuşma (TTS) motoru ile Niko size sesli olarak cevap verir.

## Kullanım Rehberi

Niko'yu kullanmaya başlamak ve onunla etkileşime geçmek oldukça doğaldır:

1.  **Başlatma:** Uygulamayı açtığınızda merkezde bulunan dinamik **Voice Orb** sizi karşılar.
2.  **Etkinleştirme:** "Niko" diyerek seslenin veya ekrandaki mikrofon simgesine dokunarak dinleme modunu aktif hale getirin.
3.  **Komut Verme:** Orb parlamaya ve sesinize göre dalgalanmaya başladığında isteğinizi söyleyin.
    - _"Ali'yi ara"_
    - _"Atatürk kimdir?"_
    - _"Atatürk kaç yılında öldü?"_
    - _"Atatürk kaç yılında doğmuştur?"_
4.  **Geri Bildirim:** Niko, talebinizi işledikten sonra hem sesli olarak yanıt verir hem de görsel animasyonlarla etkileşimi sürdürür.

## Kullanılan Teknolojiler

- Java (Android Native)
- Android Speech Recognizer & TextToSpeech
- NotificationListenerService (WhatsApp entegrasyonu için)
- HTTP URL Connection (AI Backend iletişimi için)

## Tasarım ve İkonlar

Uygulama için hazırlanan logo/ikon çalışmaları ve referans görseller:

|                        Önizleme                        | Dosya Adı               |
| :----------------------------------------------------: | :---------------------- |
| <img src="./icons/icon_candidate_01.png" width="80" /> | `icon_candidate_01.png` |
| <img src="./icons/icon_candidate_02.png" width="80" /> | `icon_candidate_02.png` |
| <img src="./icons/icon_candidate_03.png" width="80" /> | `icon_candidate_03.png` |
| <img src="./icons/icon_candidate_04.png" width="80" /> | `icon_candidate_04.png` |
|  <img src="./icons/icon_reference.jpeg" width="80" />  | `icon_reference.jpeg`   |

## Proje Dosya Yapısı

Proje içerisindeki temel dosyalar ve görevleri şunlardır:

- **`MainActivity.java`**: Uygulamanın beyni. Ses tanıma, metin okuma (TTS), API istekleri ve WhatsApp bildirim dinleme servislerini yönetir.
- **`AndroidManifest.xml`**: Uygulamanın kimliği. Gerekli izinleri (internet, mikrofon, kişilere erişim vb.) ve servisleri (NotificationListener) tanımlar.
- **`activity_main.xml`**: Ana ekran tasarımı. Voice Orb animasyonunu ve mikrofon butonunu içeren kullanıcı arayüzü (UI) dosyasıdır.
- **`orb_gradient.xml`**: Ses asistanının "gözü" olan Voice Orb için kullanılan radyal gradyan çizimidir.
- **`orb_halo.xml`**: Voice Orb'un etrafındaki yumuşak parıltı (halo) efektini sağlayan çizim dosyasıdır.
- **`mic_button.xml`**: Mikrofon butonunun görsel stilini belirleyen çizim dosyasıdır.
- **`icons/`**: Uygulama ikon adaylarını ve referans görselleri içeren klasördür.

## Yakında Eklenecekler

- [ ] Daha gelişmiş doğal dil işleme (NLP) yetenekleri.
- [ ] Özelleştirilebilir arayüz temaları ve renk seçenekleri.
- [ ] Spotify ve diğer müzik çalarlar için kontrol desteği.
- [ ] Çevrimdışı (Offline) basit komut desteği.
- [ ] Hatırlatıcı ve alarm kurma özellikleri.
- [ ] Çoklu dil desteği (İngilizce, Almanca vb.).
- [ ] Google Takvim ve Outlook entegrasyonu.
- [ ] Hava durumu ve güncel haberler için sesli özet.
- [ ] Sohbet Geçmişi.
- [ ] Chat arayüzü.
- [ ] Developer için debug ekranı.

## İletişim

Geliştirici ile iletişime geçmek için proje sayfasını ziyaret edebilirsiniz.
