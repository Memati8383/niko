const chatHistory = document.getElementById("chat-history");
const messageInput = document.getElementById("message-input");
const sendBtn = document.getElementById("send-btn");
const searchToggle = document.getElementById("search-toggle");
const typingIndicator = document.getElementById("typing-indicator");
const sidebar = document.getElementById("sidebar");
const sidebarOverlay = document.getElementById("sidebar-overlay");
const menuToggle = document.getElementById("menu-toggle");
const historyItemsContainer = document.getElementById("history-items");
const clearAllBtn = document.getElementById("clear-all-btn");
const newChatBtn = document.getElementById("new-chat-btn");
const modelSelect = document.getElementById("model-select");
const modeSelect = document.getElementById("mode-select");

// Modal Elements
const modalOverlay = document.getElementById("modal-overlay");
const modalTitle = document.getElementById("modal-title");
const modalDescription = document.getElementById("modal-description");
const modalCancelBtn = document.getElementById("modal-cancel");
const modalConfirmBtn = document.getElementById("modal-confirm");

let isWebSearchActive = false;
let currentChatId = null;
let currentMode = localStorage.getItem("selectedMode") || "normal";

// Yapılandırma
const API_URL = "/chat";
const API_KEY = "test"; // Arka uç bu özel anahtarı gerektirir

// --- Yardımcı Fonksiyonlar ---

function scrollToBottom() {
  chatHistory.scrollTo({ top: chatHistory.scrollHeight, behavior: 'smooth' });
}

async function fetchModels() {
  try {
    const response = await fetch("/models", {
      headers: { "x-api-key": API_KEY }
    });
    const data = await response.json();
    if (data.models && data.models.length > 0) {
      modelSelect.innerHTML = "";
      
      // Kayıtlı modeli localStorage'dan al
      const savedModel = localStorage.getItem("selectedModel");
      
      data.models.forEach(model => {
        const option = document.createElement("option");
        option.value = model;
        option.textContent = model;
        
        // Öncelik: 1. Kullanıcının son seçimi, 2. Backend'den gelen default
        if (savedModel) {
            if (model === savedModel) option.selected = true;
        } else if (model === data.default) {
            option.selected = true;
        }
        
        modelSelect.appendChild(option);
      });
      
      // Değişiklik olduğunda kaydet
      modelSelect.addEventListener('change', (e) => {
        localStorage.setItem("selectedModel", e.target.value);
      });
    }
  } catch (error) {
    console.error("Modeller yüklenemedi:", error);
  }
}

function addCopyButtons(container) {
  const codeBlocks = container.querySelectorAll('pre');
  codeBlocks.forEach((pre) => {
    // Eğer zaten buton varsa ekleme
    if (pre.querySelector('.copy-code-btn')) return;

    const button = document.createElement('button');
    button.className = 'copy-code-btn';
    button.innerHTML = `
      <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="9" y="9" width="13" height="13" rx="2" ry="2"></rect><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"></path></svg>
      Kopyala
    `;

    pre.style.position = 'relative';
    pre.appendChild(button);

    button.addEventListener('click', async () => {
      const code = pre.querySelector('code').innerText;
      try {
        await navigator.clipboard.writeText(code);
        
        // Buton durumunu güncelle
        button.classList.add('copied');
        const originalHTML = button.innerHTML;
        
        button.innerHTML = `
          <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"></polyline></svg>
          Kopyalandı
        `;
        
        setTimeout(() => {
          button.classList.remove('copied');
          button.innerHTML = originalHTML;
        }, 1500);
      } catch (err) {
        console.error('Kopyalama hatası:', err);
      }
    });
  });
}

function toggleSidebar(open) {
  if (open === undefined) open = !sidebar.classList.contains("open");
  
  if (open) {
    sidebar.classList.add("open");
    sidebarOverlay.classList.add("visible");
  } else {
    sidebar.classList.remove("open");
    sidebarOverlay.classList.remove("visible");
  }
}

function startNewChat() {
  chatHistory.innerHTML = "";
  currentChatId = null;
  
  // Aktif sınıfları temizle
  document.querySelectorAll('.history-item').forEach(item => item.classList.remove('active'));

  appendMessage("Merhaba! Yeni bir sohbet başlattık. Sana nasıl yardımcı olabilirim?", "bot");
  
  if (window.innerWidth <= 768) toggleSidebar(false);
}

/**
 * Özel Onay Modalı
 * @param {string} title - Modal başlığı
 * @param {string} description - Modal açıklaması
 * @returns {Promise<boolean>} - Kullanıcı onayı
 */
function showConfirmModal(title, description) {
  return new Promise((resolve) => {
    modalTitle.textContent = title;
    modalDescription.textContent = description;
    modalOverlay.classList.add("open");

    const handleConfirm = () => {
      cleanup();
      resolve(true);
    };

    const handleCancel = () => {
      cleanup();
      resolve(false);
    };

    const handleKeyDown = (e) => {
      if (e.key === "Escape") handleCancel();
    };

    const handleOverlayClick = (e) => {
      if (e.target === modalOverlay) handleCancel();
    };

    const cleanup = () => {
      modalOverlay.classList.remove("open");
      modalConfirmBtn.removeEventListener("click", handleConfirm);
      modalCancelBtn.removeEventListener("click", handleCancel);
      window.removeEventListener("keydown", handleKeyDown);
      modalOverlay.removeEventListener("click", handleOverlayClick);
    };

    modalConfirmBtn.addEventListener("click", handleConfirm);
    modalCancelBtn.addEventListener("click", handleCancel);
    window.addEventListener("keydown", handleKeyDown);
    modalOverlay.addEventListener("click", handleOverlayClick);
  });
}

// --- Mesaj Yönetimi ---

async function appendMessage(text, sender, thought = "", isStreaming = false) {
  const messageDiv = document.createElement("div");
  messageDiv.classList.add("message", sender);

  if (sender === "bot") {
    // Önce varsa düşünce bloğunu ekle
    if (thought) {
      const thoughtBlock = document.createElement("div");
      thoughtBlock.classList.add("thought-block", "collapsed");
      
      thoughtBlock.innerHTML = `
        <div class="thought-label" title="Düşünce sürecini göster/gizle">
          <div class="thought-label-left">
            <svg xmlns="http://www.w3.org/2000/svg" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 2a3 3 0 0 0-3 3v7a3 3 0 0 0 6 0V5a3 3 0 0 0-3-3Z"/><path d="M19 10v2a7 7 0 0 1-14 0v-2"/><line x1="12" x2="12" y1="19" y2="22"/><line x1="8" x2="16" y1="22" y2="22"/></svg>
            Düşünce Süreci
          </div>
          <svg class="thought-toggle-icon" xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="6 9 12 15 18 9"></polyline></svg>
        </div>
        <div class="thought-content-wrapper">
          <div class="thought-content">${thought}</div>
        </div>
      `;

      thoughtBlock.querySelector(".thought-label").addEventListener("click", () => {
        thoughtBlock.classList.toggle("collapsed");
      });

      messageDiv.appendChild(thoughtBlock);
    }

    // Ana içerik için bir container oluştur
    const contentDiv = document.createElement("div");
    contentDiv.classList.add("bot-content");
    messageDiv.appendChild(contentDiv);
    chatHistory.appendChild(messageDiv);

    if (isStreaming) {
      let currentIdx = 0;
      const streamSpeed = 15; // ms per character
      
      return new Promise((resolve) => {
        const interval = setInterval(() => {
          if (currentIdx <= text.length) {
            const partialText = text.slice(0, currentIdx);
            if (typeof marked !== "undefined" && marked.parse) {
              contentDiv.innerHTML = marked.parse(partialText);
            } else {
              contentDiv.innerHTML = `<p>${partialText}</p>`;
            }
            
            // Kod bloklarını her seferinde boyamak performansı etkileyebilir ama son adımda şart
            if (typeof hljs !== "undefined") {
               contentDiv.querySelectorAll('pre code').forEach((block) => {
                 hljs.highlightElement(block);
               });
            }

            scrollToBottom();
            currentIdx++;
          } else {
            clearInterval(interval);
            addCopyButtons(contentDiv);
            resolve();
          }
        }, streamSpeed);
      });
    } else {
      if (typeof marked !== "undefined" && marked.parse) {
        contentDiv.innerHTML = marked.parse(text);
      } else {
        contentDiv.innerHTML = `<p>${text}</p>`;
      }

      if (typeof hljs !== "undefined") {
        contentDiv.querySelectorAll('pre code').forEach((block) => {
          hljs.highlightElement(block);
        });
      }
      addCopyButtons(contentDiv);
    }
  } else {
    messageDiv.innerText = text; 
    chatHistory.appendChild(messageDiv);
  }

  scrollToBottom();
}

async function sendMessage() {
  const text = messageInput.value.trim();
  if (!text) return;

  // Eğer ilk mesajsa ve boş bir ekran varsa temizle (Hoşgeldin mesajını kaldır)
  if (!currentChatId && chatHistory.children.length <= 2) {
    chatHistory.innerHTML = "";
  }

  messageInput.value = "";
  messageInput.style.height = '52px';
  messageInput.focus();

  appendMessage(text, "user");

  typingIndicator.style.display = "flex";
  chatHistory.appendChild(typingIndicator);
  scrollToBottom();

  try {
    const response = await fetch(API_URL, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "x-api-key": API_KEY,
      },
      body: JSON.stringify({ 
        message: text, 
        enable_audio: false, 
        web_search: isWebSearchActive,
        session_id: currentChatId,
        model: modelSelect.value,
        mode: modeSelect.value
      }),
    });

    if (!response.ok) throw new Error(`Sunucu Hatası: ${response.status}`);

    const data = await response.json();
    
    // Cevap geldikten hemen sonra indicator'ı kaldır
    if (typingIndicator && typingIndicator.parentNode) {
      typingIndicator.parentNode.removeChild(typingIndicator);
    }

    if (data.reply || data.thought) {
      await appendMessage(data.reply, "bot", data.thought || "", true);
      
      const isNewChat = !currentChatId;
      currentChatId = data.id; 
      
      if (isNewChat) {
          // Yeni sohbet başladı, geçmişi yenile ve bu öğeyi aktif yap
          await fetchHistory();
      } else {
          // Mevcut sohbet devam ediyor, sadece sidebar'daki tarihi/başlığı güncelleyebiliriz
          // Şimdilik basitlik için tümünü yeniliyoruz
          fetchHistory();
      }
    } else {
      await appendMessage("Boş bir cevap alındı.", "bot");
    }
  } catch (error) {
    typingIndicator.style.display = "none";
    appendMessage(`Hata: ${error.message}`, "bot");
    console.error(error);
  }
}

// --- Geçmiş Yönetimi ---

async function fetchHistory() {
  try {
    const response = await fetch("/history", {
      headers: { "x-api-key": API_KEY }
    });
    const history = await response.json();
    renderHistory(history);
  } catch (error) {
    console.error("Geçmiş yüklenemedi:", error);
  }
}

function renderHistory(items) {
  historyItemsContainer.innerHTML = "";
  
  if (items.length === 0) {
    historyItemsContainer.innerHTML = '<div style="text-align:center; color:var(--text-muted); padding:20px; font-size:0.85rem;">Henüz geçmiş yok.</div>';
    return;
  }

  items.forEach(item => {
    const div = document.createElement("div");
    div.className = "history-item";
    if (item.id === currentChatId) div.classList.add("active");
    
    const date = new Date(item.timestamp).toLocaleString("tr-TR", {
      day: "2-digit",
      month: "2-digit",
      hour: "2-digit",
      minute: "2-digit"
    });

    div.innerHTML = `
      <div class="history-content">
        <div class="history-title" title="${item.title}">${item.title}</div>
        <div class="history-date">${date}</div>
      </div>
      <button class="delete-item-btn" data-id="${item.id}" title="Sil">
        <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M3 6h18"/><path d="M19 6v14c0 1-1 2-2 2H7c-1 0-2-1-2-2V6"/><path d="M8 6V4c0-1 1-2 2-2h4c1 0 2 1 2 2v2"/></svg>
      </button>
    `;

    div.addEventListener("click", (e) => {
      if (e.target.closest(".delete-item-btn")) return;
      loadHistoryItem(item);
    });

    const deleteBtn = div.querySelector(".delete-item-btn");
    deleteBtn.addEventListener("click", async (e) => {
      e.stopPropagation();
      const confirmed = await showConfirmModal(
        "Sohbeti Sil", 
        "Bu sohbeti silmek istediğinizden emin misiniz? Bu işlem geri alınamaz."
      );
      if (confirmed) {
        await deleteHistoryItem(item.id);
      }
    });

    historyItemsContainer.appendChild(div);
  });
}

function loadHistoryItem(item) {
  if (currentChatId === item.id) {
    if (window.innerWidth <= 768) toggleSidebar(false);
    return;
  }

  currentChatId = item.id;
  chatHistory.innerHTML = "";
  
  // Tüm mesajları sırayla ekle
  if (item.messages && item.messages.length > 0) {
    item.messages.forEach(msg => {
      appendMessage(msg.content, msg.role, msg.thought || "");
    });
  } else {
    // Eski tekli mesaj formatıyla veya boş mesajla uyumluluk
    if (item.user_message) appendMessage(item.user_message, "user");
    if (item.bot_reply) appendMessage(item.bot_reply, "bot", item.thought);
  }
  
  // Sidebar'da aktif öğeyi işaretle
  document.querySelectorAll('.history-item').forEach(el => {
      const btn = el.querySelector('.delete-item-btn');
      if (btn && btn.getAttribute('data-id') === item.id) {
          el.classList.add('active');
      } else {
          el.classList.remove('active');
      }
  });

  if (window.innerWidth <= 768) toggleSidebar(false);
}

async function deleteHistoryItem(id) {
  try {
    const response = await fetch(`/history/${id}`, {
      method: "DELETE",
      headers: { "x-api-key": API_KEY }
    });
    if (response.ok) {
      if (currentChatId === id) startNewChat();
      fetchHistory();
    }
  } catch (error) {
    console.error("Silme hatası:", error);
  }
}

async function clearAllHistory() {
  const confirmed = await showConfirmModal(
    "Tüm Geçmişi Temizle", 
    "Tüm sohbet geçmişiniz kalıcı olarak silinecektir. Devam etmek istiyor musunuz?"
  );
  if (confirmed) {
    try {
      const response = await fetch("/history", {
        method: "DELETE",
        headers: { "x-api-key": API_KEY }
      });
      if (response.ok) {
        startNewChat();
        fetchHistory();
      }
    } catch (error) {
      console.error("Geçmiş temizleme hatası:", error);
    }
  }
}

// --- Olay Dinleyicileri ---

sendBtn.addEventListener("click", sendMessage);
newChatBtn.addEventListener("click", startNewChat);
clearAllBtn.addEventListener("click", clearAllHistory);
menuToggle.addEventListener("click", () => toggleSidebar());
sidebarOverlay.addEventListener("click", () => toggleSidebar(false));

searchToggle.addEventListener("click", () => {
  isWebSearchActive = !isWebSearchActive;
  searchToggle.classList.toggle("active", isWebSearchActive);
  messageInput.placeholder = isWebSearchActive ? "Web'de ara..." : "Bir şeyler sorun...";
});

messageInput.addEventListener("keydown", (e) => {
  if (e.key === "Enter" && !e.shiftKey) {
    e.preventDefault();
    sendMessage();
  }
});

messageInput.addEventListener('input', function() {
  this.style.height = 'auto';
  this.style.height = (this.scrollHeight) + 'px';
  if(this.value === '') this.style.height = '52px';
});

// --- Mod Seçimi Event Listener ---
modeSelect.addEventListener("change", (e) => {
  currentMode = e.target.value;
  localStorage.setItem("selectedMode", currentMode);
  
  // Mod değiştiğinde görsel feedback
  const modeMessages = {
    "normal": "🤖 Normal mod aktif. Yardımsever ve profesyonel cevaplar alacaksınız.",
    "agresif": "🔥 DİKKAT: Agresif mod aktif! Bu mod yetişkin içerik barındırabilir.",
    "bilge": "📜 Bilge mod aktif. Felsefi ve derin düşünceli bir sohbet sizi bekliyor.",
    "dahi": "🧠 Dahi mod aktif. Teknik ve analitik bir zeka ile konuşuyorsunuz.",
    "kibar": "🎩 Kibar mod aktif. Zarif ve nazik bir İstanbul beyefendisi hizmetinizde.",
    "esprili": "🤣 Esprili mod aktif. Hazırcevap ve eğlenceli esprilere hazır olun!",
    "kodlayici": "💻 Kodlayıcı mod aktif. Sistem optimize edildi, buglar temizlendi.",
    "romantik": "💖 Romantik mod aktif. Kalpleri ısıtan şiirsel bir sohbet başlıyor."
  };
  
  // Mod değişikliğini kullanıcıya bildir (sadece sohbet aktifse)
  if (chatHistory.children.length > 0) {
    const notification = document.createElement("div");
    notification.className = "mode-notification";
    notification.innerHTML = modeMessages[currentMode] || "Mod değiştirildi.";
    chatHistory.appendChild(notification);
    scrollToBottom();
    
    // Bildirim 3 saniye sonra kaybolsun
    setTimeout(() => {
      notification.classList.add("fade-out");
      setTimeout(() => notification.remove(), 500);
    }, 3000);
  }
});

// Başlangıç
window.addEventListener("DOMContentLoaded", () => {
  messageInput.style.height = '52px';
  fetchHistory();
  fetchModels();
  
  // Kaydedilmiş modu yükle
  const savedMode = localStorage.getItem("selectedMode");
  if (savedMode && modeSelect) {
    modeSelect.value = savedMode;
    currentMode = savedMode;
  }
  
  // İlk yüklemede, eğer URL'de veya başka bir yerde session yoksa ve geçmiş boşsa hoşgeldin mesajı
  setTimeout(() => {
    if (chatHistory.children.length === 0 && !currentChatId) {
      appendMessage("Merhaba! Ben Niko. Sana nasıl yardımcı olabilirim?", "bot");
    }
  }, 500);
});
