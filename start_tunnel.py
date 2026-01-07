<<<<<<< HEAD
import subprocess
import re
import sys
import os

def update_readme(url):
    readme_path = "README.md"
    try:
        with open(readme_path, "r", encoding="utf-8") as f:
            lines = f.readlines()

        new_lines = []
        updated = False
        for line in lines:
            if line.strip().startswith("> 🌐 **Güncel Tünel Adresi:**"):
                new_line = f"> 🌐 **Güncel Tünel Adresi:** [{url}]({url})\n"
                new_lines.append(new_line)
                updated = True
            else:
                new_lines.append(line)

        if updated:
            with open(readme_path, "w", encoding="utf-8") as f:
                f.writelines(new_lines)
            print(f"\n[+] README.md güncellendi: {url}")
        else:
            # Satır bulunamadıysa dosyanın sonuna ekle
            with open(readme_path, "a", encoding="utf-8") as f:
                f.write(f"\n> 🌐 **Güncel Tünel Adresi:** [{url}]({url})\n")
            print(f"\n[+] README.md sonuna yeni adres eklendi: {url}")
            
    except Exception as e:
        print(f"\n[!] Dosya güncelleme hatası: {e}")

def main():
    cmd = ["cloudflared", "tunnel", "--url", "http://127.0.0.1:8000"]
    print(f"[*] Başlatılıyor: {' '.join(cmd)}")
    print("[*] URL bekleniyor...")

    try:
        # cloudflared genellikle linki stderr'e yazar
        process = subprocess.Popen(
            cmd,
            stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT,
            text=True,
            bufsize=1,
            encoding='utf-8'
        )

        url_regex = re.compile(r"https://[a-zA-Z0-9-]+\.trycloudflare\.com")
        
        while True:
            line = process.stdout.readline()
            if not line:
                break
            
            print(line.strip())
            
            match = url_regex.search(line)
            if match:
                url = match.group(0)
                update_readme(url)
                # URL bulunduktan sonra döngüyü kırmaya gerek yok, tünel açık kalmalı
                # Ancak sürekli update etmeye de gerek yok, belki flag koyabiliriz.
                # Genelde URL değişmez process çalışırken.
                
    except KeyboardInterrupt:
        print("\n[*] İşlem kullanıcı tarafından durduruldu.")
        process.terminate()
    except FileNotFoundError:
        print("\n[!] Hata: 'cloudflared' komutu bulunamadı. Lütfen yüklü olduğundan ve PATH'e eklendiğinden emin olun.")
    except Exception as e:
        print(f"\n[!] Beklenmeyen hata: {e}")

if __name__ == "__main__":
    main()
=======
import subprocess
import re
import sys
import os

def update_readme(url):
    readme_path = "README.md"
    try:
        with open(readme_path, "r", encoding="utf-8") as f:
            lines = f.readlines()

        new_lines = []
        updated = False
        for line in lines:
            if line.strip().startswith("> 🌐 **Güncel Tünel Adresi:**"):
                new_line = f"> 🌐 **Güncel Tünel Adresi:** [{url}]({url})\n"
                new_lines.append(new_line)
                updated = True
            else:
                new_lines.append(line)

        if updated:
            with open(readme_path, "w", encoding="utf-8") as f:
                f.writelines(new_lines)
            print(f"\n[+] README.md güncellendi: {url}")
        else:
            # Satır bulunamadıysa dosyanın sonuna ekle
            with open(readme_path, "a", encoding="utf-8") as f:
                f.write(f"\n> 🌐 **Güncel Tünel Adresi:** [{url}]({url})\n")
            print(f"\n[+] README.md sonuna yeni adres eklendi: {url}")
            
    except Exception as e:
        print(f"\n[!] Dosya güncelleme hatası: {e}")

def main():
    cmd = ["cloudflared", "tunnel", "--url", "http://127.0.0.1:8000"]
    print(f"[*] Başlatılıyor: {' '.join(cmd)}")
    print("[*] URL bekleniyor...")

    try:
        # cloudflared genellikle linki stderr'e yazar
        process = subprocess.Popen(
            cmd,
            stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT,
            text=True,
            bufsize=1,
            encoding='utf-8'
        )

        url_regex = re.compile(r"https://[a-zA-Z0-9-]+\.trycloudflare\.com")
        
        while True:
            line = process.stdout.readline()
            if not line:
                break
            
            print(line.strip())
            
            match = url_regex.search(line)
            if match:
                url = match.group(0)
                update_readme(url)
                # URL bulunduktan sonra döngüyü kırmaya gerek yok, tünel açık kalmalı
                # Ancak sürekli update etmeye de gerek yok, belki flag koyabiliriz.
                # Genelde URL değişmez process çalışırken.
                
    except KeyboardInterrupt:
        print("\n[*] İşlem kullanıcı tarafından durduruldu.")
        process.terminate()
    except FileNotFoundError:
        print("\n[!] Hata: 'cloudflared' komutu bulunamadı. Lütfen yüklü olduğundan ve PATH'e eklendiğinden emin olun.")
    except Exception as e:
        print(f"\n[!] Beklenmeyen hata: {e}")

if __name__ == "__main__":
    main()
>>>>>>> b554b426b90ac16dd9878d0ce1c1cfbc5da6215a
