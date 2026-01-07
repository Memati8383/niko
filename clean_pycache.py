<<<<<<< HEAD
import os
import shutil

def clean_all_pycache():
    deleted_count = 0
    print("🧹 Pycache temizliği başlatılıyor...")
    
    for root, dirs, files in os.walk('.'):
        # __pycache__ klasörlerini bul
        if '__pycache__' in dirs:
            pycache_path = os.path.join(root, '__pycache__')
            try:
                shutil.rmtree(pycache_path)
                print(f"🗑️ Silindi: {pycache_path}")
                deleted_count += 1
            except Exception as e:
                print(f"⚠️ Hata (Silinemedi): {pycache_path} - {e}")

    # Ek olarak .pyc ve .pyo dosyalarını da temizleyelim
    for root, dirs, files in os.walk('.'):
        for file in files:
            if file.endswith(('.pyc', '.pyo')):
                file_path = os.path.join(root, file)
                try:
                    os.remove(file_path)
                    deleted_count += 1
                except:
                    pass

    print(f"\n✨ Temizlik tamamlandı. {deleted_count} öğe kaldırıldı.")

if __name__ == "__main__":
    clean_all_pycache()
=======
import os
import shutil

def clean_all_pycache():
    deleted_count = 0
    print("🧹 Pycache temizliği başlatılıyor...")
    
    for root, dirs, files in os.walk('.'):
        # __pycache__ klasörlerini bul
        if '__pycache__' in dirs:
            pycache_path = os.path.join(root, '__pycache__')
            try:
                shutil.rmtree(pycache_path)
                print(f"🗑️ Silindi: {pycache_path}")
                deleted_count += 1
            except Exception as e:
                print(f"⚠️ Hata (Silinemedi): {pycache_path} - {e}")

    # Ek olarak .pyc ve .pyo dosyalarını da temizleyelim
    for root, dirs, files in os.walk('.'):
        for file in files:
            if file.endswith(('.pyc', '.pyo')):
                file_path = os.path.join(root, file)
                try:
                    os.remove(file_path)
                    deleted_count += 1
                except:
                    pass

    print(f"\n✨ Temizlik tamamlandı. {deleted_count} öğe kaldırıldı.")

if __name__ == "__main__":
    clean_all_pycache()
>>>>>>> b554b426b90ac16dd9878d0ce1c1cfbc5da6215a
