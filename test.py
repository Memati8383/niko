import requests

# url = "http://127.0.0.1:8000/chat"
url = "https://ron-nickname-wine-emotions.trycloudflare.com/chat"

try:
    res = requests.post(
        url,
        headers={"x-api-key": "test"},
        json={"message": "merhaba"}
    )
    res.raise_for_status()
    print(res.json())
except requests.exceptions.RequestException as e:
    print(f"Hata: {e}")
