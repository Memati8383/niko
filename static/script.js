const chatHistory = document.getElementById("chat-history");
const messageInput = document.getElementById("message-input");
const sendBtn = document.getElementById("send-btn");
const sendIcon = document.getElementById("send-icon");
const stopIcon = document.getElementById("stop-icon");
const searchToggle = document.getElementById("search-toggle");
const ragToggle = document.getElementById("rag-toggle");
const typingIndicator = document.getElementById("typing-indicator");
const sidebar = document.getElementById("sidebar");
const sidebarOverlay = document.getElementById("sidebar-overlay");
const menuToggle = document.getElementById("menu-toggle");
const historyItemsContainer = document.getElementById("history-items");
const clearAllBtn = document.getElementById("clear-all-btn");
const newChatBtn = document.getElementById("new-chat-btn");
const modelSelect = document.getElementById("model-select");
const modeSelect = document.getElementById("mode-select");
const historySearch = document.getElementById("history-search");
const chatCountEl = document.getElementById("chat-count");
const messageCountEl = document.getElementById("message-count");

// Modal Elements
const modalOverlay = document.getElementById("modal-overlay");
const modalTitle = document.getElementById("modal-title");
const modalDescription = document.getElementById("modal-description");
const modalCancelBtn = document.getElementById("modal-cancel");
const modalConfirmBtn = document.getElementById("modal-confirm");
const selectionMenu = document.getElementById("selection-menu");
const selectionBtns = document.querySelectorAll(".selection-btn");
const helpBtn = document.getElementById("help-btn");
const shortcutsModal = document.getElementById("shortcuts-modal");
const shortcutsCloseBtn = document.getElementById("shortcuts-close");
const socialLinksContainer = document.getElementById("social-links");
const socialTrigger = document.getElementById("social-trigger");

let isWebSearchActive = false;
let isRagSearchActive = false;
let currentChatId = null;
let currentMode = localStorage.getItem("selectedMode") || "normal";
let allHistoryItems = []; // Tüm geçmiş öğelerini sakla
let abortController = null; // AI cevabını durdurmak için
let isGenerating = false; // AI cevap üretiyor mu?
let shouldStopTyping = false; // Yazma animasyonunu durdurmak için
let selectedText = ""; // Seçilen metin
let selectedElement = null; // Seçimin yapıldığı element

// Yapılandırma
const API_URL = "/chat";
const API_KEY = "test"; // Arka uç bu özel anahtarı gerektirir

// --- Yardımcı Fonksiyonlar ---

function scrollToBottom() {
  chatHistory.scrollTo({ top: chatHistory.scrollHeight, behavior: 'smooth' });
}

// Debounce utility for performance optimization
function debounce(func, wait) {
  let timeout;
  return function executedFunction(...args) {
    const later = () => {
      clearTimeout(timeout);
      func(...args);
    };
    clearTimeout(timeout);
    timeout = setTimeout(later, wait);
  };
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
    
    // Kaynaklar için bir container (eğer varsa)
    const sourcesContainer = document.createElement("div");
    sourcesContainer.classList.add("sources-container");
    messageDiv.appendChild(sourcesContainer);

    chatHistory.appendChild(messageDiv);

    if (isStreaming) {
      let currentIdx = 0;
      const streamSpeed = 10; // Biraz daha hızlı animasyon
      shouldStopTyping = false;
      
      await new Promise((resolve) => {
        const interval = setInterval(() => {
          if (shouldStopTyping || !isGenerating) {
            clearInterval(interval);
            resolve();
            return;
          }

          if (currentIdx <= text.length) {
            const partialText = text.slice(0, currentIdx);
            
            // Client-side cleaning (Safety Layer)
            const cleanPartial = partialText.replace(/^\\+/g, '');

            if (typeof marked !== "undefined" && marked.parse) {
              contentDiv.innerHTML = marked.parse(cleanPartial);
            } else {
              contentDiv.innerHTML = `<p>${cleanPartial}</p>`;
            }
            
            // Performans için her karakterde değil, sadece kod bloğu bittiğinde veya periyodik olarak highlight yap
            if (typeof hljs !== "undefined" && (currentIdx % 10 === 0 || currentIdx === text.length)) {
               contentDiv.querySelectorAll('pre code').forEach((block) => {
                 hljs.highlightElement(block);
               });
            }

            scrollToBottom();
            currentIdx++;
          } else {
            clearInterval(interval);
            resolve();
          }
        }, streamSpeed);
      });
      addCopyButtons(contentDiv);
    } else {
      // Non-streaming cleanup
      const cleanText = text.replace(/^\\+/g, '');
      if (typeof marked !== "undefined" && marked.parse) {
        contentDiv.innerHTML = marked.parse(cleanText);
      } else {
        contentDiv.innerHTML = `<p>${cleanText}</p>`;
      }

      if (typeof hljs !== "undefined") {
        contentDiv.querySelectorAll('pre code').forEach((block) => {
          hljs.highlightElement(block);
        });
      }
      addCopyButtons(contentDiv);
    }

    // Kaynakları render et (Hem streaming hem non-streaming için)
    if (window.currentMessageSources && window.currentMessageSources.length > 0) {
      renderChatSources(sourcesContainer, window.currentMessageSources);
      window.currentMessageSources = null; // Temizle
    }
  } else {
    messageDiv.innerText = text; 
    chatHistory.appendChild(messageDiv);
  }

  scrollToBottom();
}

function renderChatSources(container, sources) {
  if (!sources || sources.length === 0) return;
  
  const label = document.createElement("div");
  label.className = "sources-label";
  label.innerHTML = `
    <svg xmlns="http://www.w3.org/2000/svg" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M2 3h6a4 4 0 0 1 4 4v14a3 3 0 0 0-3-3H2z"></path><path d="M22 3h-6a4 4 0 0 0-4 4v14a3 3 0 0 1 3-3h7z"></path></svg>
    Referanslar ve Kaynaklar
  `;
  container.appendChild(label);

  sources.forEach(source => {
    const item = document.createElement("div");
    item.className = `source-item ${source.type}`;
    
    let contentHtml = "";
    if (source.type === "web") {
        // Web sonuçlarını daha temiz göster (Daha basit bir görüntü)
        contentHtml = `<div class="source-type-badge web">Web</div> İnternet araması verileri kullanıldı.`;
    } else if (source.type === "rag") {
        contentHtml = `<div class="source-type-badge rag">Sağlık DB</div> Tıbbi veritabanı bağlamı kullanıldı.`;
    }
    
    item.innerHTML = contentHtml;
    container.appendChild(item);
  });
}

// UI durumunu güncelle (gönder/durdur butonu)
function updateSendButtonState(generating) {
  isGenerating = generating;
  
  if (generating) {
    sendIcon.style.display = "none";
    stopIcon.style.display = "block";
    sendBtn.setAttribute("aria-label", "Durdur");
    sendBtn.classList.add("generating");
    messageInput.disabled = true;
  } else {
    sendIcon.style.display = "block";
    stopIcon.style.display = "none";
    sendBtn.setAttribute("aria-label", "Gönder");
    sendBtn.classList.remove("generating");
    messageInput.disabled = false;
  }
}

// AI cevabını durdur
function stopGeneration() {
  shouldStopTyping = true;
  isGenerating = false;
  
  if (abortController) {
    abortController.abort();
    abortController = null;
  }
  
  updateSendButtonState(false);
  
  // Typing indicator'ı kaldır
  if (typingIndicator && typingIndicator.parentNode) {
    typingIndicator.parentNode.removeChild(typingIndicator);
  }
  
  // Kullanıcıya bilgi ver
  const stopMsg = document.createElement("div");
  stopMsg.className = "message system-warning";
  stopMsg.innerHTML = "⚠️ Cevap oluşturma durduruldu.";
  chatHistory.appendChild(stopMsg);
  scrollToBottom();
}

async function sendMessage() {
  // Eğer şu an AI cevap üretiyorsa, durdur (Giriş kutusu boş olsa bile)
  if (isGenerating) {
    stopGeneration();
    return;
  }

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
  
  // Buton durumunu güncelle
  updateSendButtonState(true);
  
  // AbortController oluştur
  abortController = new AbortController();

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
        rag_search: isRagSearchActive,
        session_id: currentChatId,
        model: modelSelect.value,
        mode: modeSelect.value
      }),
      signal: abortController.signal // Abort signal ekle
    });

    if (!response.ok) throw new Error(`Sunucu Hatası: ${response.status}`);

    const data = await response.json();
    
    // Cevap geldikten hemen sonra indicator'ı kaldır
    if (typingIndicator && typingIndicator.parentNode) {
      typingIndicator.parentNode.removeChild(typingIndicator);
    }

    if (data.reply || data.thought) {
      // Global değişkende kaynakları sakla (appendMessage tarafından kullanılacak)
      window.currentMessageSources = data.sources || [];
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
    // Eğer kullanıcı durdurduysa hata gösterme
    if (error.name === 'AbortError') {
      console.log('İstek kullanıcı tarafından durduruldu');
      return;
    }
    
    typingIndicator.style.display = "none";
    appendMessage(`Hata: ${error.message}`, "bot");
    console.error(error);
  } finally {
    // Her durumda butonu normale döndür
    updateSendButtonState(false);
    abortController = null;
  }
}

// --- Geçmiş Yönetimi ---

async function fetchHistory() {
  try {
    const response = await fetch("/history", {
      headers: { "x-api-key": API_KEY }
    });
    const history = await response.json();
    allHistoryItems = history; // Global değişkende sakla
    renderHistory(history);
    updateStats(history);
  } catch (error) {
    console.error("Geçmiş yüklenemedi:", error);
  }
}

// İstatistikleri güncelle
function updateStats(items) {
  const chatCount = items.length;
  let totalMessages = 0;
  
  items.forEach(item => {
    if (item.messages && Array.isArray(item.messages)) {
      totalMessages += item.messages.length;
    }
  });
  
  // Animasyonlu sayı güncellemesi
  animateValue(chatCountEl, parseInt(chatCountEl.textContent) || 0, chatCount, 500);
  animateValue(messageCountEl, parseInt(messageCountEl.textContent) || 0, totalMessages, 500);
}

function animateValue(element, start, end, duration) {
  const range = end - start;
  const increment = range / (duration / 16);
  let current = start;
  
  const timer = setInterval(() => {
    current += increment;
    if ((increment > 0 && current >= end) || (increment < 0 && current <= end)) {
      element.textContent = end;
      clearInterval(timer);
    } else {
      element.textContent = Math.floor(current);
    }
  }, 16);
}

// Arama fonksiyonu (debounced)
const searchHistory = debounce((query) => {
  const searchTerm = query.trim().toLowerCase();
  if (!searchTerm) {
    renderHistory(allHistoryItems);
    return;
  }
  
  const filtered = allHistoryItems.filter(item => {
    const titleMatch = item.title.toLowerCase().includes(searchTerm);
    const messageMatch = item.messages && item.messages.some(msg => 
      msg.content.toLowerCase().includes(searchTerm)
    );
    return titleMatch || messageMatch;
  });
  
  renderHistory(filtered, searchTerm);
}, 300);

function renderHistory(items, highlightTerm = "") {
  historyItemsContainer.innerHTML = "";
  
  if (items.length === 0) {
    historyItemsContainer.innerHTML = `
      <div class="empty-state">
        <svg class="empty-state-icon" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="11" cy="11" r="8"></circle><line x1="21" y1="21" x2="16.65" y2="16.65"></line></svg>
        <div class="empty-state-text">${highlightTerm ? 'Eşleşen sonuç bulunamadı.' : 'Henüz geçmiş yok.'}</div>
      </div>
    `;
    return;
  }

  items.forEach((item, index) => {
    const div = document.createElement("div");
    div.className = "history-item";
    if (item.id === currentChatId) div.classList.add("active");
    
    // Kademeli giriş animasyonu için delay ekle
    div.style.animationDelay = `${index * 50}ms`;
    
    const date = new Date(item.timestamp).toLocaleString("tr-TR", {
      day: "2-digit",
      month: "2-digit",
      hour: "2-digit",
      minute: "2-digit"
    });

    // Başlığı vurgula
    let displayTitle = item.title;
    if (highlightTerm) {
      const regex = new RegExp(`(${highlightTerm})`, 'gi');
      displayTitle = item.title.replace(regex, '<mark class="highlight-match">$1</mark>');
    }

    div.innerHTML = `
      <div class="history-content">
        <div class="history-title" title="${item.title}">${displayTitle}</div>
        <div class="history-date">${date}</div>
      </div>
      <div class="history-actions">
        <button class="export-item-btn" data-id="${item.id}" title="Dışa Aktar">
          <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" x2="12" y1="15" y2="3"/></svg>
        </button>
        <button class="delete-item-btn" data-id="${item.id}" title="Sil">
          <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M3 6h18"/><path d="M19 6v14c0 1-1 2-2 2H7c-1 0-2-1-2-2V6"/><path d="M8 6V4c0-1 1-2 2-2h4c1 0 2 1 2 2v2"/></svg>
        </button>
      </div>
    `;

    div.addEventListener("click", (e) => {
      if (e.target.closest(".delete-item-btn") || e.target.closest(".export-item-btn")) return;
      loadHistoryItem(item);
    });

    const exportBtn = div.querySelector(".export-item-btn");
    exportBtn.addEventListener("click", async (e) => {
      e.stopPropagation();
      await exportHistoryItem(item.id, item.title);
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

// Toast notification system
function showToast(message, type = 'success') {
  const toast = document.createElement('div');
  toast.className = `toast toast-${type}`;
  
  const icon = type === 'success' 
    ? '<svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"></polyline></svg>'
    : '<svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"></circle><line x1="12" y1="8" x2="12" y2="12"></line><line x1="12" y1="16" x2="12.01" y2="16"></line></svg>';
  
  toast.innerHTML = `${icon}<span>${message}</span>`;
  document.body.appendChild(toast);
  
  // Trigger animation
  setTimeout(() => toast.classList.add('show'), 10);
  
  // Auto dismiss
  setTimeout(() => {
    toast.classList.remove('show');
    setTimeout(() => toast.remove(), 300);
  }, 3000);
}

async function exportHistoryItem(id, title) {
  try {
    const response = await fetch(`/export/${id}`, {
      headers: { "x-api-key": API_KEY }
    });
    
    if (!response.ok) throw new Error("Dışa aktarma başarısız");
    
    const blob = await response.blob();
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `${title.substring(0, 30)}_${id.substring(0, 8)}.md`;
    document.body.appendChild(a);
    a.click();
    window.URL.revokeObjectURL(url);
    document.body.removeChild(a);
    
    // Success notification
    showToast('Sohbet başarıyla dışa aktarıldı!', 'success');
  } catch (error) {
    console.error("Dışa aktarma hatası:", error);
    showToast('Sohbet dışa aktarılamadı. Lütfen tekrar deneyin.', 'error');
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

helpBtn.addEventListener("click", () => {
  shortcutsModal.classList.add("open");
});

shortcutsCloseBtn.addEventListener("click", () => {
  shortcutsModal.classList.remove("open");
});

shortcutsModal.addEventListener("click", (e) => {
  if (e.target === shortcutsModal) {
    shortcutsModal.classList.remove("open");
  }
});

searchToggle.addEventListener("click", () => {
  isWebSearchActive = !isWebSearchActive;
  searchToggle.classList.toggle("active", isWebSearchActive);
  updatePlaceholder();
});

ragToggle.addEventListener("click", () => {
  isRagSearchActive = !isRagSearchActive;
  ragToggle.classList.toggle("active", isRagSearchActive);
  updatePlaceholder();
});

function updatePlaceholder() {
  if (isWebSearchActive && isRagSearchActive) {
    messageInput.placeholder = "Web + Sağlık DB'de ara...";
  } else if (isWebSearchActive) {
    messageInput.placeholder = "Web'de ara...";
  } else if (isRagSearchActive) {
    messageInput.placeholder = "Sağlık Veritabanı'nda ara...";
  } else {
    messageInput.placeholder = "Bir şeyler sorun...";
  }
}

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

// --- Klavye Kısayolları ---
window.addEventListener("keydown", (e) => {
  // Escape tuşu ile durdurma
  if (e.key === "Escape" && isGenerating) {
    stopGeneration();
  }
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
    "romantik": "💖 Romantik mod aktif. Kalpleri ısıtan şiirsel bir sohbet başlıyor.",
    "rag": "🏥 Sağlık (RAG) modu aktif. Tıbbi veritabanı bağlamı öncelikli kullanılacak."
  };
  
  // RAG Modu seçildiğinde toggle'ı da aktif et (veya tam tersi)
  if (currentMode === "rag") {
    isRagSearchActive = true;
    ragToggle.classList.add("active");
  } else if (!isRagSearchActive) {
    // Eğer manuel olarak açılmamışsa kapat (mod değiştirdiğinde RAG modundan çıkılmışsa)
    // Ancak kullanıcı manuel açtıysa kapatmıyoruz
  }
  updatePlaceholder();

  // Mod değişikliğini kullanıcıya bildir (sadece sohbet aktifse)
  if (chatHistory.children.length > 0) {
    const notification = document.createElement("div");
    notification.className = "mode-notification";
    if (currentMode === "agresif") notification.classList.add("agresif");
    if (currentMode === "rag") notification.classList.add("rag"); // RAG moduna özel stil (varsa)
    
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

// Arama event listener
historySearch.addEventListener('input', (e) => {
  searchHistory(e.target.value);
});

// Klavye kısayolları
window.addEventListener('keydown', (e) => {
  // Ctrl+K veya Cmd+K: Arama kutusuna odaklan
  if ((e.ctrlKey || e.metaKey) && e.key === 'k') {
    e.preventDefault();
    if (sidebar.classList.contains('open')) {
      historySearch.focus();
    } else {
      toggleSidebar(true);
      setTimeout(() => historySearch.focus(), 100);
    }
  }
  
  // Ctrl+N veya Cmd+N: Yeni sohbet
  if ((e.ctrlKey || e.metaKey) && e.key === 'n') {
    e.preventDefault();
    startNewChat();
  }
  
  // Escape: Sidebar'ı kapat
  if (e.key === 'Escape' && sidebar.classList.contains('open')) {
    toggleSidebar(false);
  }
});

// --- Seçim Menüsü Logic ---

function handleSelection() {
  const selection = window.getSelection();
  const text = selection.toString().trim();
  
  if (text.length > 3) {
    const range = selection.getRangeAt(0);
    const rect = range.getBoundingClientRect();
    
    // Sadece bot mesajları veya kullanıcı mesajları içindeki seçimlerde göster
    const container = range.commonAncestorContainer.parentElement.closest('.message');
    if (!container) {
      hideSelectionMenu();
      return;
    }

    selectedText = text;
    selectedElement = container;

    selectionMenu.style.display = 'flex';
    
    // Menü konumu (seçimin üstünde ortala)
    const menuWidth = selectionMenu.offsetWidth || 300; // Öngörülen genişlik
    const left = rect.left + (rect.width / 2) - (menuWidth / 2);
    const top = rect.top - 50 + window.scrollY;

    selectionMenu.style.left = `${Math.max(10, Math.min(left, window.innerWidth - menuWidth - 10))}px`;
    selectionMenu.style.top = `${top - 10}px`;
  } else {
    hideSelectionMenu();
  }
}

function hideSelectionMenu() {
  selectionMenu.style.display = 'none';
  selectedText = "";
}

// Seçim olaylarını dinle
document.addEventListener("mouseup", (e) => {
  if (selectionMenu.contains(e.target)) return;
  setTimeout(handleSelection, 10);
});

document.addEventListener("mousedown", (e) => {
  if (!selectionMenu.contains(e.target)) {
    hideSelectionMenu();
  }
});

// Aksiyonları tanımla
selectionBtns.forEach(btn => {
  btn.addEventListener("click", async () => {
    const action = btn.getAttribute("data-action");
    if (!selectedText) return;

    if (action === "copy") {
      try {
        await navigator.clipboard.writeText(selectedText);
        showToast("Seçilen metin kopyalandı!");
      } catch (err) {
        showToast("Kopyalanamadı.", "error");
      }
    } else {
      let prompt = "";
      switch(action) {
        case "explain":
          prompt = `Şu metni bana daha detaylı açıklar mısın: "${selectedText}"`;
          break;
        case "summarize":
          prompt = `Şu metni kısaca özetler misın: "${selectedText}"`;
          break;
        case "translate":
          prompt = `Şu metni İngilizce'ye (veya metin İngilizce ise Türkçe'ye) çevirir misin: "${selectedText}"`;
          break;
      }
      
      messageInput.value = prompt;
      sendMessage();
    }
    
    hideSelectionMenu();
    window.getSelection().removeAllRanges();
  });
});

// --- Sosyal Medya Menüsü ---
if (socialTrigger) {
  socialTrigger.addEventListener("click", (e) => {
    e.stopPropagation();
    socialLinksContainer.classList.toggle("active");
  });

  document.addEventListener("click", (e) => {
    if (!socialLinksContainer.contains(e.target)) {
      socialLinksContainer.classList.remove("active");
    }
  });
}

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
    const hasMessages = chatHistory.querySelectorAll('.message').length > 0;
    if (!hasMessages && !currentChatId) {
      appendMessage("Merhaba! Ben Niko. Sana nasıl yardımcı olabilirim?", "bot");
    }
  }, 500);
});
