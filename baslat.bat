@echo off
TITLE Niko AI - Sistem Baslatici

echo ==========================================
echo       Niko AI Sistemi Baslatiliyor
echo ==========================================

:: 1. Ollama Servisini Baslat
echo [1/3] Ollama servisi baslatiliyor...
:: Ollama'nin halihazirda calisip calismadigini kontrol etmeye gerek yok, 
:: 'serve' komutu zaten calisiyorsa hata verip kapanir.
start "Ollama" cmd /k "ollama serve"
timeout /t 5 /nobreak > nul

:: 2. Ana Uygulama Sunucusunu Baslat (Port 8000)
echo [2/3] Ana sunucu (main.py) baslatiliyor...
start "Niko Main Server" cmd /k "python main.py"
timeout /t 5 /nobreak > nul

:: 3. Cloudflare Tunnel Baslat
echo [3/3] Cloudflare Tunnel ve GitHub Guncelleme baslatiliyor...
start "Niko Tunnel" cmd /k "python start_tunnel.py"

echo.
echo ------------------------------------------
echo Tum bilesenler ayri pencerelerde baslatildi!
echo ------------------------------------------
echo.
echo NOT: Ollama zaten calisiyorsa o pencerede hata gorebilirsiniz, onemli degil.
echo.
pause
