<<<<<<< HEAD
import requests
import json
import time

def test_chat():
    url = "http://localhost:8000/chat"
    
    # main.py içindeki API_KEY kontrolü için header ekliyoruz
    # API_KEY varsayılan olarak "test" olarak ayarlanmış
    headers = {
        "Content-Type": "application/json",
        "X-API-Key": "test"
    }

    # ChatRequest modeline uygun payload
    payload = {
        "message": "Merhaba, sistem testi yapıyorum. Lütfen kısa bir yanıt ver.",
        "enable_audio": False,
        "web_search": False,
        "rag_search": False,
        "mode": "normal"
    }

    print(f"🚀 İstek atılıyor: {url}...")
    start_time = time.time()
    
    try:
        # main.py şu an stream=False olarak yapılandırılmış (satır 310)
        # Bu yüzden standart bir POST isteği atıyoruz
        response = requests.post(url, json=payload, headers=headers, timeout=60)
        duration = time.time() - start_time
        
        print(f"📡 Durum Kodu: {response.status_code} ({duration:.2f} saniye)")
        
        if response.status_code == 200:
            result = response.json()
            reply = result.get("reply", "")
            thought = result.get("thought", "")
            session_id = result.get("id", "")
            
            print("\n" + "="*50)
            if thought:
                print(f"💭 DÜŞÜNCE SÜRECİ:\n{thought}\n")
                print("-" * 30)
            
            print(f"🤖 ASİSTAN YANITI:\n{reply}")
            print("="*50)
            print(f"\n✅ Test Başarılı! (Session ID: {session_id})")
        
        elif response.status_code == 401:
            print("❌ Hata: Yetkisiz erişim! API Key hatalı.")
        elif response.status_code == 503:
            print("❌ Hata: Ollama servisine ulaşılamıyor!")
        else:
            print(f"❌ Hata: {response.status_code}")
            print(response.text)
            
    except requests.exceptions.ConnectionError:
        print("❌ Hata: Sunucuya bağlanılamadı! main.py çalışıyor mu?")
    except Exception as e:
        print(f"❌ Beklenmedik Hata: {e}")

if __name__ == "__main__":
    test_chat()
=======
import requests
import json
import time

def test_chat():
    url = "http://localhost:8000/chat"
    
    # main.py içindeki API_KEY kontrolü için header ekliyoruz
    # API_KEY varsayılan olarak "test" olarak ayarlanmış
    headers = {
        "Content-Type": "application/json",
        "X-API-Key": "test"
    }

    # ChatRequest modeline uygun payload
    payload = {
        "message": "Merhaba, sistem testi yapıyorum. Lütfen kısa bir yanıt ver.",
        "enable_audio": False,
        "web_search": False,
        "rag_search": False,
        "mode": "normal"
    }

    print(f"🚀 İstek atılıyor: {url}...")
    start_time = time.time()
    
    try:
        # main.py şu an stream=False olarak yapılandırılmış (satır 310)
        # Bu yüzden standart bir POST isteği atıyoruz
        response = requests.post(url, json=payload, headers=headers, timeout=60)
        duration = time.time() - start_time
        
        print(f"📡 Durum Kodu: {response.status_code} ({duration:.2f} saniye)")
        
        if response.status_code == 200:
            result = response.json()
            reply = result.get("reply", "")
            thought = result.get("thought", "")
            session_id = result.get("id", "")
            
            print("\n" + "="*50)
            if thought:
                print(f"💭 DÜŞÜNCE SÜRECİ:\n{thought}\n")
                print("-" * 30)
            
            print(f"🤖 ASİSTAN YANITI:\n{reply}")
            print("="*50)
            print(f"\n✅ Test Başarılı! (Session ID: {session_id})")
        
        elif response.status_code == 401:
            print("❌ Hata: Yetkisiz erişim! API Key hatalı.")
        elif response.status_code == 503:
            print("❌ Hata: Ollama servisine ulaşılamıyor!")
        else:
            print(f"❌ Hata: {response.status_code}")
            print(response.text)
            
    except requests.exceptions.ConnectionError:
        print("❌ Hata: Sunucuya bağlanılamadı! main.py çalışıyor mu?")
    except Exception as e:
        print(f"❌ Beklenmedik Hata: {e}")

if __name__ == "__main__":
    test_chat()
>>>>>>> b554b426b90ac16dd9878d0ce1c1cfbc5da6215a
