const chatHistory = document.getElementById("chat-history");
const messageInput = document.getElementById("message-input");
const sendBtn = document.getElementById("send-btn");
const typingIndicator = document.getElementById("typing-indicator");


// Yapılandırma
const API_URL = "/chat";
// Arka uç bu özel anahtarı gerektirir
const API_KEY = "test";

function scrollToBottom() {
  chatHistory.scrollTo({ top: chatHistory.scrollHeight, behavior: 'smooth' });
}

// Metin alanını otomatik boyutlandır
messageInput.addEventListener('input', function() {
  this.style.height = 'auto';
  this.style.height = (this.scrollHeight) + 'px';
  if(this.value === '') this.style.height = '52px';
});

function appendMessage(text, sender, thought = "") {
  const messageDiv = document.createElement("div");
  messageDiv.classList.add("message", sender);

  if (sender === "bot") {
    let contentHtml = "";

    // Varsa Düşünce Bloğu Ekle
    if (thought) {
      contentHtml += `
        <div class="thought-block">
          <div class="thought-label">
            <svg xmlns="http://www.w3.org/2000/svg" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 2a3 3 0 0 0-3 3v7a3 3 0 0 0 6 0V5a3 3 0 0 0-3-3Z"/><path d="M19 10v2a7 7 0 0 1-14 0v-2"/><line x1="12" x2="12" y1="19" y2="22"/><line x1="8" x2="16" y1="22" y2="22"/></svg>
            Düşünce Süreci
          </div>
          <div class="thought-content">${thought}</div>
        </div>
      `;
    }

    // Yanıt için Markdown Ayrıştırma ve Sözdizimi Vurgulama
    if (typeof marked !== "undefined" && marked.parse) {
      contentHtml += marked.parse(text);
    } else {
      contentHtml += `<p>${text}</p>`;
    }

    messageDiv.innerHTML = contentHtml;

    // highlight.js'yi kod bloklarına uygula
    if (typeof hljs !== "undefined") {
      messageDiv.querySelectorAll('pre code').forEach((block) => {
        hljs.highlightElement(block);
      });
    }

  } else {
    // Kullanıcı mesajları için boşlukları koru
    messageDiv.innerText = text; 
  }

  chatHistory.appendChild(messageDiv);
  scrollToBottom();
}

async function sendMessage() {
  const text = messageInput.value.trim();
  if (!text) return;

  // Girdiyi temizle ve resetle
  messageInput.value = "";
  messageInput.style.height = '52px'; // Yüksekliği sıfırla
  messageInput.focus();

  // Kullanıcı mesajını ekle
  appendMessage(text, "user");

  // Yazıyor... göstergesini aç
  typingIndicator.style.display = "flex";
  chatHistory.appendChild(typingIndicator); // En alta taşı
  scrollToBottom();

  try {
    const response = await fetch(API_URL, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "x-api-key": API_KEY,
      },
      body: JSON.stringify({ message: text, enable_audio: false }),
    });

    if (!response.ok) {
      throw new Error(`Sunucu Hatası: ${response.status}`);
    }

    const data = await response.json();

    // Yazıyor... göstergesini gizle
    typingIndicator.style.display = "none";

    if (data.reply || data.thought) {
      appendMessage(data.reply, "bot", data.thought || "");
    } else {
      appendMessage("Boş bir cevap alındı.", "bot");
    }
  } catch (error) {
    typingIndicator.style.display = "none";
    appendMessage(`Hata: ${error.message}`, "bot");
    console.error(error);
  }
}

// Olay Dinleyicileri (Event Listeners)
sendBtn.addEventListener("click", sendMessage);

messageInput.addEventListener("keydown", (e) => {
  if (e.key === "Enter" && !e.shiftKey) {
    e.preventDefault();
    sendMessage();
  }
});

// Başlangıç Karşılaması
window.addEventListener("DOMContentLoaded", () => {
  // İlk yüklemede textarea yüksekliğini ayarla
  messageInput.style.height = '52px';
  
  setTimeout(() => {
     appendMessage(
    "Merhaba! Ben Niko. Sana nasıl yardımcı olabilirim?",
    "bot"
  );
  }, 500); // Hafif gecikme ile daha doğal durur
});
